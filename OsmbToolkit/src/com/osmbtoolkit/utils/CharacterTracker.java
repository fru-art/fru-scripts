package com.osmbtoolkit.utils;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.minimap.Minimap;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.drawing.SceneProjector;
import com.osmbtoolkit.script.ToolkitScript;

import java.awt.Color;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CharacterTracker {
  private final ToolkitScript script;
  private final Map<String, TrackedPlayer> trackedPlayers = new ConcurrentHashMap<>();
  private final Runnable frameListener = this::onFrame;
  private final Consumer<Canvas> paintListener = this::onPaint;

  private long lastFrameTime = 0;
  private long lastRequestTime = 0;
  private long frameCount = 0; // Incremented every frame to track "freshness"
  private boolean isListening = false;
  private boolean isEagerTracking = false;

  private static final double MAX_TILES_PER_TICK = 5.0;
  private static final long STALENESS_MS = 5000;
  private static final long PLAYER_TTL_MS = 1800;
  private static final double TICK_MS_DOUBLE = 600.0;

  public CharacterTracker(ToolkitScript script) {
    this.script = script;
  }

  /**
   * ONLY returns players that were physically found in the current frame
   */
  public Map<String, WorldPosition> getPlayerPositions() {
    markActivity();
    Map<String, WorldPosition> results = new HashMap<>();
    trackedPlayers.forEach((uuid, player) -> {
      if (player.lastUpdateFrame == frameCount) {
        results.put(uuid, player.position);
      }
    });
    return results;
  }

  public Optional<WorldPosition> getPlayerPosition(String uuid) {
    markActivity();
    TrackedPlayer p = trackedPlayers.get(uuid);
    if (p != null && p.lastUpdateFrame == frameCount) {
      return Optional.of(p.position);
    }
    return Optional.empty();
  }

  public Map<String, WorldPosition> getStillPlayerPositions() {
    markActivity();
    Map<String, WorldPosition> results = new HashMap<>();
    trackedPlayers.forEach((uuid, player) -> {
      if (player.lastUpdateFrame == frameCount && player.stillTicks >= 2 && isNearInteger(player.position)) {
        results.put(uuid, player.position);
      }
    });
    return results;
  }

  // ... (debug, markActivity, destroy methods)

  /**
   * Implicitly enables eager tracking if debugging
   */
  public void debug(boolean shouldDebug) {
    script.removePaintListener(paintListener);
    if (shouldDebug) script.addPaintListener(paintListener);
  }

  public void destroy() {
    if (isListening) {
      script.removeFrameListener(frameListener);
      isListening = false;
    }
    script.removePaintListener(paintListener);
    trackedPlayers.clear();
  }

  public void disableEagerTracking() {
    isEagerTracking = false;
  }

  public void enableEagerTracking() {
    isEagerTracking = true;
    markActivity();
  }

  private void markActivity() {
    this.lastRequestTime = System.currentTimeMillis();
    if (!isListening) {
      script.addFrameListener(frameListener);
      isListening = true;
    }
  }

  private void onFrame() {
    long now = System.currentTimeMillis();
    if (now - lastRequestTime > STALENESS_MS) {
      destroy();
      return;
    }

    frameCount++; // New frame starts

    WidgetManager widgetManager = script.getWidgetManager();
    if (widgetManager == null) return;
    Minimap minimap = widgetManager.getMinimap();
    if (minimap == null) return;

    List<WorldPosition> currentPositions = minimap.getPlayerPositions().asList();
    if (currentPositions == null) currentPositions = Collections.emptyList();

    double deltaTicks = (lastFrameTime == 0) ? 1.0 : (now - lastFrameTime) / TICK_MS_DOUBLE;
    lastFrameTime = now;

    List<WorldPosition> unmatchedNew = new ArrayList<>(currentPositions);

    // 1. MATCHING: Attempt to reconcile new positions with existing tracked players/ghosts
    // We iterate through existing objects. If matched, the ID is preserved.
    for (TrackedPlayer player : trackedPlayers.values()) {
      WorldPosition bestMatch = null;
      double bestScore = Double.MAX_VALUE;

      // Use velocity to project where we expect them to be
      double projectedX = player.position.getPreciseX() + (player.vx * deltaTicks);
      double projectedY = player.position.getPreciseY() + (player.vy * deltaTicks); // Fixed vx -> vy

      for (WorldPosition pos : unmatchedNew) {
        double scoreDist = getChebyshevDistance(projectedX, projectedY, pos.getPreciseX(), pos.getPreciseY());
        double actualMoveDist = getChebyshevDistance(player.position.getPreciseX(), player.position.getPreciseY(),
          pos.getPreciseX(), pos.getPreciseY());

        if (actualMoveDist > MAX_TILES_PER_TICK * deltaTicks) continue;

        if (scoreDist < bestScore) {
          bestScore = scoreDist;
          bestMatch = pos;
        }
      }

      if (bestMatch != null) {
        // Ghost is reclaimed or active player is updated. Identity preserved.
        player.update(bestMatch, deltaTicks, now, frameCount);
        unmatchedNew.remove(bestMatch);
      }
    }

    // 2. CLEANUP: Delete players who haven't been seen (even as ghosts) in 1.8 seconds
    trackedPlayers.values().removeIf(p -> now - p.lastSeenTimestamp > PLAYER_TTL_MS);

    // 3. REGISTRATION: Any position not claimed by an existing ID gets a new one
    for (WorldPosition newPos : unmatchedNew) {
      String newUuid = UUID.randomUUID().toString();
      trackedPlayers.put(newUuid, new TrackedPlayer(newUuid, newPos, now, frameCount));
    }

    if (isEagerTracking) markActivity();
  }

  private void onPaint(Canvas canvas) {
    if (!script.isGameScreenVisible()) return;

    // ... (Drawing logic remains the same, it uses getPlayerPositions() which is now filtered)
    Map<String, WorldPosition> playerPositions = getPlayerPositions();
    Map<String, WorldPosition> stillPlayerPositions = getStillPlayerPositions();

    for (String uuid : playerPositions.keySet()) {
      WorldPosition position = playerPositions.get(uuid);
      Optional<Polygon> polygon = script.getPlayerCube(position);
      SceneProjector sceneProjector = script.getSceneProjector();
      Point textPoint = (sceneProjector == null || position == null) ? null : sceneProjector.getTilePoint(position, null, 200);

      if (polygon.isPresent()) {
        Paint.drawPolygon(canvas, polygon.get(), stillPlayerPositions.containsKey(uuid) ? Color.LIGHT_GRAY : Color.GRAY);
      }
      if (textPoint != null) {
        Paint.drawSmallText(canvas, uuid.substring(0, 4), textPoint.x, textPoint.y, Color.WHITE, true);
      }
    }
  }

  private static class TrackedPlayer {
    String uuid;
    WorldPosition position;
    long lastSeenTimestamp;
    long lastUpdateFrame; // The frame count where this player was last "Active"
    double vx = 0, vy = 0;
    int stillTicks = 0;

    TrackedPlayer(String uuid, WorldPosition pos, long now, long frame) {
      this.uuid = uuid;
      this.position = pos;
      this.lastSeenTimestamp = now;
      this.lastUpdateFrame = frame;
    }

    void update(WorldPosition newPos, double deltaTicks, long now, long frame) {
      double dx = newPos.getPreciseX() - position.getPreciseX();
      double dy = newPos.getPreciseY() - position.getPreciseY();

      this.vx = (dx / deltaTicks) * 0.8 + (vx * 0.2);
      this.vy = (dy / deltaTicks) * 0.8 + (vy * 0.2);

      if (Math.abs(dx) < 0.05 && Math.abs(dy) < 0.05) {
        stillTicks++;
      } else {
        stillTicks = 0;
      }
      this.position = newPos;
      this.lastSeenTimestamp = now;
      this.lastUpdateFrame = frame;
    }
  }

  private double getChebyshevDistance(double x1, double y1, double x2, double y2) {
    return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
  }

  private boolean isNearInteger(WorldPosition pos) {
    return Math.abs(pos.getPreciseX() - Math.round(pos.getPreciseX())) < 0.15 &&
      Math.abs(pos.getPreciseY() - Math.round(pos.getPreciseY())) < 0.15;
  }
}
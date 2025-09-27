import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.overlay.HealthOverlay;
import com.osmb.api.walker.pathing.CollisionManager;
import helper.DrawHelper;
import helper.WaitHelper;
import task.Task;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillGoblinTask extends Task {
  private static final RectangleArea BUILDING = new RectangleArea(3243, 3244, 5, 4, 0);

  private final DrawHelper drawHelper;
  private final WaitHelper waitHelper;

  private final Random random;

  public KillGoblinTask(Script script) {
    super(script);

    drawHelper = new DrawHelper(script);
    waitHelper = new WaitHelper(script);

    random = new Random();
  }

  @Override
  public boolean canExecute() {
    return script.getWidgetManager().getMinimapOrbs().getHitpoints() >= 6;
  }

  @Override
  public boolean execute() {
    HealthOverlay healthOverlay = new HealthOverlay(script);

    if (!isFighting(healthOverlay)) {
      attackGoblin(healthOverlay);
    }

    if (!script.submitHumanTask(healthOverlay::isVisible, 400, false, true)) {
      script.log(getClass(), "Failed to instantiate health overlay");
      return false;
    }

    boolean killed = waitHelper.waitForNoChange(
      "Npc health",
      () -> getHealthOverlayHitpoints(healthOverlay),
      8_000, // Should not take more than 8s to damage goblin
      16_000, // Should not take more than 16s to kill goblin
      () -> {
        if (!healthOverlay.isVisible()) {
          script.log(getClass(), "Killed goblin by way of health overlay no longer visible");
          return true;
        }
        Integer hitpoints = getHealthOverlayHitpoints(healthOverlay);
        if (hitpoints == null) {
          script.log(getClass(), "Killed goblin by way of hitpoints being null");
          return true;
        }
        if (hitpoints == 0) {
          script.log(getClass(), "Killed goblin by reducing hitpoints to 0");
          return true;
        }
        return false;
      },
      true);

    if (!killed) {
      script.log(getClass(), "Failed to kill goblin");
      return false;
    }

    return true;
  }

  private boolean attackGoblin(HealthOverlay healthOverlay) {
    // Check for idle before grabbing positions of NPCs
    if (!waitHelper.waitForNoChange(
      "Position",
      script::getWorldPosition,
      1_000,
      3_000)) {
      script.log(getClass(), "Failed to stop moving");
      return false;
    }

    WorldPosition position = script.getWorldPosition();
    List<WorldPosition> npcs = script.getWidgetManager().getMinimap().getNPCPositions().asList();
    List<WorldPosition> players = script.getWidgetManager().getMinimap().getPlayerPositions().asList();

    List<WorldPosition> unoccupiedNpcs = new ArrayList<>();

    for (WorldPosition npc : npcs) {
      if (BUILDING.contains(npc)) continue;
      // Heuristic for ignoring recently killed NPC
      if (healthOverlay.isVisible() && CollisionManager.isCardinallyAdjacent(npc, position)) {
        continue;
      }

      boolean occupied = false;
      for (WorldPosition player : players) {
        if (CollisionManager.isCardinallyAdjacent(npc, player)) {
          drawHelper.drawPosition(npc, Color.RED);
          occupied = true;
          break;
        }
      }
      if (occupied) continue;

      drawHelper.drawPosition("npc=" + npc, npc, Color.GREEN);
      unoccupiedNpcs.add(npc);
    }

    if (unoccupiedNpcs.isEmpty()) {
      script.log(getClass(), "Failed to find any unoccupied NPCs. Hopping worlds");
      script.getProfileManager().forceHop();
      return false;
    }

    unoccupiedNpcs = unoccupiedNpcs.stream()
      .sorted(Comparator.comparingDouble(npc -> npc.distanceTo(position)))
      .limit(random.nextInt(3, 5))
      .toList();

    if (unoccupiedNpcs.isEmpty()) {
      script.log(getClass(), "Failed to find unoccupied goblins");
      return false;
    }

    WorldPosition attackedNpc = null;
    for (WorldPosition unoccupiedNpc : unoccupiedNpcs) {
      Shape tilePoly;
      try {
        tilePoly = script.getSceneProjector().getTileCube(unoccupiedNpc, 50).getResized(0.7);
      } catch (NullPointerException e) {
        continue;
      }

      // Use 'for' loop (performance) to double-check that NPC is still in the right location post-processing and
      // pre-tap
      boolean stillNpc = false;
      for (WorldPosition nowNpc : script.getWidgetManager().getMinimap().getNPCPositions()) {
        if (nowNpc.equals(unoccupiedNpc)) {
          stillNpc = true;
          break;
        }
      }

      if (!stillNpc) {
        script.getScreen().removeCanvasDrawable("npc=" + unoccupiedNpc);
        drawHelper.drawPosition("npc=" + unoccupiedNpc, unoccupiedNpc, Color.RED);
        continue;
      }

      if (script.getFinger().tapGameScreen(tilePoly, menuEntries -> menuEntries.stream()
        .filter(item ->
          "attack goblin".equalsIgnoreCase(item.getRawText()) ||
            "attack giant spider".equalsIgnoreCase(item.getRawText()))
        .findFirst()
        .orElse(null))) {
        attackedNpc = unoccupiedNpc;
        break;
      }
    }

    if (attackedNpc == null) {
      script.log(getClass(), "Failed to attack goblin");
      return false;
    }

    if (!waitHelper.waitForNoChange(
      "Position",
      script::getWorldPosition,
      1_000,
      (int) position.distanceTo(attackedNpc) * 1_000)) {
      script.log(getClass(), "Failed to reach goblin");
      return false;
    }

    return true;
  }

  private Integer getHealthOverlayHitpoints(HealthOverlay healthOverlay) {
    HealthOverlay.HealthResult healthResult = (HealthOverlay.HealthResult) healthOverlay.getValue(HealthOverlay.HEALTH);
    if (healthResult == null) return null;
    return healthResult.getCurrentHitpoints();
  }

  private boolean isFighting(HealthOverlay healthOverlay) {
    if (healthOverlay == null || !healthOverlay.isVisible()) return false;
    Integer hitpoints = getHealthOverlayHitpoints(healthOverlay);
    return hitpoints != null && hitpoints > 0;
  }
}

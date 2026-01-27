package com.osmbtoolkit.utils;

import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.CollisionMap;
import com.osmb.api.scene.ObjectManager;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.SceneManager;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.utils.TileEdge;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.walker.Walker;
import com.osmb.api.walker.pathing.CollisionFlags;
import com.osmbtoolkit.script.ToolkitScript;

import java.awt.Color;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Door {
  public enum DoorState {OPEN, CLOSED}

  private static final Map<Direction.PrimaryDirection, TileEdge> TILE_EDGE_MAP = Map.of(
    Direction.PrimaryDirection.N,
    TileEdge.N,
    Direction.PrimaryDirection.E,
    TileEdge.E,
    Direction.PrimaryDirection.S,
    TileEdge.S,
    Direction.PrimaryDirection.W,
    TileEdge.W);

  private final TileEdge closedEdge;
  private final int height;
  private final TileEdge openEdge;
  private final Consumer<Canvas> paintListener;
  private final int plane;
  private final ToolkitScript script;
  private final SearchablePixel[] searchablePixels;
  private final double thickness;
  private final WorldPosition worldPosition;

  /**
   * @param thickness Thickness relative to the size of one tile e.g. 0.5 means half the length of a tile. This
   *                  can be visualized with the tile cube scale tool.
   */
  public Door(ToolkitScript script,
              WorldPosition position,
              Direction.PrimaryDirection openEdge,
              Direction.PrimaryDirection closedEdge,
              SearchablePixel[] searchablePixels,
              double thickness,
              int height,
              int plane) {
    this.closedEdge = TILE_EDGE_MAP.get(closedEdge);
    assert this.closedEdge != null;
    this.height = height;
    this.openEdge = TILE_EDGE_MAP.get(openEdge);
    assert this.openEdge != null;
    this.paintListener = this::draw;
    this.plane = plane;
    this.script = script;
    this.searchablePixels = searchablePixels;
    this.thickness = thickness;
    this.worldPosition = position;
  }

  public void debug() {
    debug(true);
  }

  public void debug(boolean on) {
    if (on) {
      script.addPaintListener(paintListener);
    } else {
      script.removePaintListener(paintListener);
    }
  }

  public Optional<DoorState> getState() {
    Optional<RSObject> object = getRSObject();
    if (object.isEmpty() || !object.get().isInteractableOnScreen()) return Optional.empty();

    Optional<Polygon> openSearchShapeOptional = getPolygon(DoorState.OPEN);
    if (openSearchShapeOptional.isEmpty()) return Optional.empty();
    Polygon openSearchShape = openSearchShapeOptional.get();
    Optional<Polygon> closedSearchShapeOptional = getPolygon(DoorState.CLOSED);
    if (closedSearchShapeOptional.isEmpty()) return Optional.empty();
    Polygon closedSearchShape = closedSearchShapeOptional.get();

    Optional<PixelCluster> openCluster = script.findLargestCluster(openSearchShape, this.searchablePixels);
    double openDetectionRatio =
      openCluster.map(pixelCluster -> pixelCluster.getPoints().size() / openSearchShape.calculateArea()).orElse(0.0);
    Optional<PixelCluster> closedCluster = script.findLargestCluster(closedSearchShape, this.searchablePixels);
    double closedDetectionRatio =
      closedCluster.map(pixelCluster -> pixelCluster.getPoints().size() / closedSearchShape.calculateArea())
        .orElse(0.0);

    if (Math.abs(openDetectionRatio - closedDetectionRatio) < 0.05) return Optional.empty();
    return Optional.of(openDetectionRatio > closedDetectionRatio ? DoorState.OPEN : DoorState.CLOSED);
  }

  public Optional<Polygon> getPolygon(DoorState doorState) {
    Optional<LocalPosition> localPosition = getLocalPosition();
    if (localPosition.isEmpty()) return Optional.empty();

    TileEdge edge = (doorState == DoorState.OPEN) ? openEdge : closedEdge;
    Polygon polygon = new Polygon();
    double halfT = thickness / 2.0;

    // Determine where the door "sits" on the tile
    if (edge == TileEdge.N) {
      // North edge is at Y = 1.0
      addPrismToPolygon(polygon, localPosition.get(), 0, 1, 1.0 - halfT, 1.0 + halfT);
    } else if (edge == TileEdge.S) {
      // South edge is at Y = 0.0
      addPrismToPolygon(polygon, localPosition.get(), 0, 1, -halfT, halfT);
    } else if (edge == TileEdge.E) {
      // East edge is at X = 1.0
      addPrismToPolygon(polygon, localPosition.get(), 1.0 - halfT, 1.0 + halfT, 0, 1);
    } else if (edge == TileEdge.W) {
      // West edge is at X = 0.0
      addPrismToPolygon(polygon, localPosition.get(), -halfT, halfT, 0, 1);
    } else {
      // Shouldn't ever happen. Debugging https://discord.com/channels/736938454478356570/1415051321425526784/1465170930568396926
      script.log(getClass(), "Failed to get polygon from edges: " + openEdge + " " + closedEdge);
      return Optional.empty();
    }

    Polygon hull = polygon.convexHull();
    // Shouldn't ever happen. Debugging https://discord.com/channels/736938454478356570/1415051321425526784/1465170930568396926
    if (hull == null) {
      script.log(
        getClass(),
        "Failed to get hull from points: " + Arrays.toString(polygon.getXPoints()) + " " + Arrays.toString(polygon.getYPoints()));
      return Optional.empty();
    }
    return Optional.of(hull);
  }

  public boolean interact(DoorState desiredState) {
    // We target the polygon of the CURRENT state to change it
    // (If we want it OPEN, we look for the CLOSED polygon)
    BooleanSupplier isClickable = () -> {
      Optional<Polygon> poly = getPolygon(desiredState == DoorState.OPEN ? DoorState.CLOSED : DoorState.OPEN);
      return poly.isPresent() && poly.get().insideGameScreenFactor(script) >= 0.8;
    };

    // If not visible/clickable, walk until it is
    if (!isClickable.getAsBoolean()) {
      if (!walkTo(isClickable)) return false;
    }

    Optional<Polygon> poly = getPolygon(desiredState == DoorState.OPEN ? DoorState.CLOSED : DoorState.OPEN);
    if (poly.isEmpty()) return false;

    return script.getFinger().tapGameScreen(
      poly.get(), (menuEntries) -> {
        String action = (desiredState == DoorState.OPEN) ? "open" : "close";
        String altAction = (desiredState == DoorState.OPEN) ? "open" : "shut";

        return menuEntries.stream()
          .filter(e -> e.getAction().toLowerCase().contains(action) || e.getAction().toLowerCase().contains(altAction))
          .findFirst()
          .orElse(null);
      });
  }

  public boolean passTo(Position destination) {
    return passTo(destination, null, 0);
  }

  public boolean passTo(Position destination, BooleanSupplier breakCondition) {
    return passTo(destination, breakCondition, 0);
  }

  private boolean passTo(Position destination, BooleanSupplier breakCondition, int retryCount) {
    if (retryCount > 3) return false;

    // 1. Walk to door, break when < 5 distance (improves state detection)
    walkTo(() -> script.getWorldPosition().distanceTo(worldPosition) < 5);

    // 2. If state is determinable and looks closed, try to open (don't wait for validation)
    Optional<DoorState> state = getState();
    if (state.isPresent() && state.get() == DoorState.CLOSED) {
      script.log(getClass(), "Door detected as closed, interacting");
      interact(DoorState.OPEN);
    }

    // 3. Update collision map and try to walk through
    if (!removeCollision()) {
      script.log(getClass(), "Failed to remove door from collision map");
      return false;
    }

    script.log(getClass(), "Attempting to pass through door");
    double startDist = getPerpendicularDistance().orElse(0.0);
    long startTime = System.currentTimeMillis();
    AtomicBoolean crossed = new AtomicBoolean(false);

    WalkConfig config = new WalkConfig.Builder().breakCondition(() -> {
      if (breakCondition != null && breakCondition.getAsBoolean()) return true;

      if (System.currentTimeMillis() - startTime > 2_400 && !crossed.get()) {
        script.log(getClass(), "Failed to cross door within 4 ticks");
        return true;
      }

      // Logic: If the perpendicular distance sign flips, we are on the other side
      getPerpendicularDistance().ifPresent(curr -> {
        if ((curr * startDist < 0) || (Math.abs(curr) > 0 && startDist == 0)) {
          crossed.set(true);
        }
      });
      return crossed.get();
    }).build();

    script.getWalker().walkTo(destination, config);

    // 4. If fails to walk through (didn't cross), try to open again then recurse
    if (!crossed.get()) {
      if (interact(DoorState.OPEN)) {
        script.log(getClass(), "Successfully opened door after failing to cross. Re-attempting pass");
        return passTo(destination, breakCondition, retryCount + 1);
      }
      return false;
    }

    return true;
  }

  public boolean walkTo() {
    return walkTo(null);
  }

  public boolean walkTo(BooleanSupplier breakCondition) {
    WorldPosition position = script.getWorldPosition();
    if (position != null && position.distanceTo(worldPosition) <= 0.1) return true;

    Walker walker = script.getWalker();
    if (walker == null) return false;

    WalkConfig walkConfig = new WalkConfig.Builder().breakCondition(breakCondition).build();
    return walker.walkTo(worldPosition, walkConfig);
  }

  private void addPrismToPolygon(Polygon poly,
                                 LocalPosition localPosition,
                                 double minX,
                                 double maxX,
                                 double minY,
                                 double maxY) {
    int[] heights = {0, this.height};
    double[] xOffsets = {minX, maxX};
    double[] yOffsets = {minY, maxY};

    for (int h : heights) {
      for (double dx : xOffsets) {
        for (double dy : yOffsets) {
          Point p = script.getSceneProjector()
            .getTilePoint(localPosition.getPreciseX() + dx, localPosition.getPreciseY() + dy, plane, TileEdge.SW, h);
          if (p != null) poly.addVertex(p.x, p.y);
        }
      }
    }
  }

  private void draw(Canvas canvas) {
    WidgetManager widgetManager = script.getWidgetManager();
    GameState gameState = widgetManager == null ? null : widgetManager.getGameState();
    if (gameState != GameState.LOGGED_IN) return;

    Optional<DoorState> doorState = getState();
    Color color = doorState.isEmpty() ? Color.ORANGE : doorState.get() == DoorState.CLOSED ? Color.RED : Color.GREEN;
    Optional<Polygon> polygon = doorState.isPresent() && doorState.get() == DoorState.OPEN ?
      getPolygon(DoorState.OPEN) :
      getPolygon(DoorState.CLOSED);
    if (polygon.isEmpty()) return;

    canvas.drawPolygon(polygon.get(), color.getRGB(), 0.6);
    canvas.fillPolygon(polygon.get(), color.getRGB(), 0.3);
  }

  private Optional<LocalPosition> getLocalPosition() {
    return getLocalPosition(false);
  }

  private Optional<LocalPosition> getLocalPosition(boolean roundToNearest) {
    SceneManager sceneManager = script.getSceneManager();
    if (sceneManager == null) return Optional.empty();
    double x = worldPosition.getPreciseX();
    double y = worldPosition.getPreciseY();
    double localX = (roundToNearest ? Math.round(x) : x) - sceneManager.getSceneBaseTileX();
    double localY = (roundToNearest ? Math.round(y) : y) - sceneManager.getSceneBaseTileY();
    // Not within current or neighboring region
    if (localX >= 128 || localX <= -64 || localY >= 128 || localY <= -64) return Optional.empty();
    return Optional.of(new LocalPosition(localX, localY, worldPosition.getPlane()));
  }

  private Optional<Double> getPerpendicularDistance() {
    WorldPosition worldPosition = script.getWorldPosition();
    if (worldPosition == null) return Optional.empty();
    LocalPosition localPosition = worldPosition.toLocalPosition(script);
    if (localPosition == null) return Optional.empty();
    return getPerpendicularDistance(localPosition);
  }

  private Optional<Double> getPerpendicularDistance(LocalPosition localPosition) {
    Optional<LocalPosition> doorPosition = getLocalPosition();
    if (doorPosition.isEmpty()) return Optional.empty();
    double preciseDoorX = doorPosition.get().getPreciseX();
    double preciseDoorY = doorPosition.get().getPreciseY();

    if (localPosition == null) return Optional.empty();
    double preciseX = localPosition.getPreciseX();
    double preciseY = localPosition.getPreciseY();

    if (closedEdge == TileEdge.N || closedEdge == TileEdge.S) {
      return Optional.of(preciseY - preciseDoorY + (closedEdge == TileEdge.N ? -0.5 : 0.5));
    } else {
      return Optional.of(preciseX - preciseDoorX + (closedEdge == TileEdge.W ? -0.5 : 0.5));
    }
  }

  private Optional<RSObject> getRSObject() {
    ObjectManager objectManager = script.getObjectManager();
    if (objectManager == null) return Optional.empty();

    List<RSObject> doorObjects = objectManager.getObjects((object) -> {
      // Near provided world position
      if (object.getWorldPosition().distanceTo(worldPosition) > 2) return false;
      // Includes open or close action
      String[] actions = object.getActions();
      if (actions == null || actions.length == 0) return false;
      return Stream.of(actions)
        .anyMatch(action -> action != null && (action.equalsIgnoreCase("Open") || action.equalsIgnoreCase("Close")));
    });

    if (doorObjects == null || doorObjects.isEmpty()) return Optional.empty();

    return doorObjects.stream().min(Comparator.comparingDouble(object -> object.distance(worldPosition)));
  }

  private boolean removeCollision() {
    Optional<LocalPosition> localPosition = getLocalPosition(true);
    if (localPosition.isEmpty()) return false;
    SceneManager sceneManager = script.getSceneManager();
    if (sceneManager == null) return false;
    CollisionMap collisionMap = sceneManager.getLevelCollisionMap(plane);
    if (collisionMap == null) return false;

    int x = localPosition.get().getX();
    int y = localPosition.get().getY();
    int otherX = closedEdge == TileEdge.W ? x - 1 : closedEdge == TileEdge.E ? x + 1 : x;
    int otherY = closedEdge == TileEdge.N ? y + 1 : closedEdge == TileEdge.S ? y - 1 : y;

    collisionMap.flags[x][y] = CollisionFlags.OPEN;
    collisionMap.flags[otherX][otherY] = CollisionFlags.OPEN;
    return true;
  }
}

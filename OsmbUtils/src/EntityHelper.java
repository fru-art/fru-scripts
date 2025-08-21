import com.osmb.api.ScriptCore;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.visual.PixelAnalyzer.RespawnCircle;
import com.osmb.api.visual.PixelAnalyzer.RespawnCircleDrawType;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use 'ObjectHelper' nad 'PlayerHelper'
 */
@Deprecated
public class EntityHelper {
  private final DrawHelper drawHelper;
  private final Script script;

  public EntityHelper(Script script) {
    this.drawHelper = new DrawHelper(script);
    this.script = script;
  }

  public List<RSObject> getNamedObjects(List<String> expectedNames) {
    return script.getObjectManager().getObjects((RSObject object) -> {
      String name = object.getName();
      return name != null &&
        expectedNames.stream().anyMatch(expectedName -> name.compareToIgnoreCase(expectedName) == 0);
    });
  }
  public List<RSObject> getNamedObjects(String expectedName) {
    return getNamedObjects(List.of(expectedName));
  }

  public List<RSObject> filterUnspawnedObjects(List<RSObject> objects,
                                               RespawnCircleDrawType drawType,
                                               int zOffset,
                                               int distanceTolerance) {
    Map<RSObject, RespawnCircle> objectToRespawnCircleMap = script
      .getPixelAnalyzer()
      .getRespawnCircleObjects(objects, drawType, zOffset, distanceTolerance);
    Set<RSObject> unspawnedObjects = objectToRespawnCircleMap.keySet();

    return objects.stream().filter((object) -> !unspawnedObjects.contains(object)).toList();
  }

  public List<RSObject> filterOccupiedObjects(
    ScriptCore scriptCore,
    List<RSObject> objects,
    boolean debug,
    Set<WorldPosition> ignorePositionsToDraw
  ) {
    Set<WorldPosition> playerPositions = new HashSet<>(getOtherPlayerPositions(debug));

    return objects.stream()
      .filter((object) -> {
        List<WorldPosition> adjacentPositions = getAdjacentPositions(scriptCore, object);
        for (WorldPosition adjacentPosition: adjacentPositions) {
          if (playerPositions.contains(adjacentPosition)) {
            return false;
          }

          if (debug && ignorePositionsToDraw != null && !ignorePositionsToDraw.contains(adjacentPosition)) {
            drawHelper.drawPosition(adjacentPosition, Color.BLUE);
          }
        }
        return true;
      }).toList();
  }
  public List<RSObject> filterOccupiedObjects(ScriptCore scriptCore, List<RSObject> objects) {
    return  filterOccupiedObjects(scriptCore, objects, false, null);
  }
  public List<RSObject> filterOccupiedObjects(ScriptCore scriptCore, List<RSObject> objects, boolean debug) {
    return  filterOccupiedObjects(
      scriptCore,
      objects,
      debug,
      debug ?
        objects.stream()
          .flatMap(object -> object.getObjectArea().getAllWorldPositions().stream())
          .collect(Collectors.toSet()) :
        null
    );
  }

  public List<WorldPosition> getAdjacentPositions(ScriptCore scriptCore, RSObject object, boolean debug) {
    List<WorldPosition> objectPositions = object.getObjectArea().getAllWorldPositions();
    Set<Integer> objectXs = objectPositions.stream().map(Position::getX).collect(Collectors.toSet());
    Set<Integer> objectYs = objectPositions.stream().map(Position::getY).collect(Collectors.toSet());

    List<WorldPosition> adjacentPositions = object.getSurroundingPositions().stream()
      .map(position -> position.toWorldPosition(scriptCore))
      .filter(position -> objectXs.contains(position.getX()) || objectYs.contains(position.getY()))
      .toList();

    if (debug) {
      for (WorldPosition position: adjacentPositions) {
        drawHelper.drawPosition("surroundingPosition", position, Color.BLUE);
      }
    }

    return adjacentPositions;
  }
  public List<WorldPosition> getAdjacentPositions(ScriptCore scriptCore, RSObject object) {
    return getAdjacentPositions(scriptCore, object, false);
  }

  public List<WorldPosition> getOtherPlayerPositions(boolean debug) {
    List<WorldPosition> positions = script
      .getWidgetManager()
      .getMinimap()
      .getPlayerPositions()
      .asList();

    if (debug) {
      for (WorldPosition position : positions) {
        drawHelper.drawPosition("otherPlayer", position, Color.LIGHT_GRAY);
      }
    }

    return positions;
  }
  public List<WorldPosition> getOtherPlayerPositions() {
    return getOtherPlayerPositions(false);
  }

  public boolean isPlayerIdling() {
    return !script.getPixelAnalyzer().isPlayerAnimating(0.25);
  }
}

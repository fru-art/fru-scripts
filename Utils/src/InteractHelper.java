import com.osmb.api.ScriptCore;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.visual.PixelAnalyzer.RespawnCircle;
import com.osmb.api.visual.PixelAnalyzer.RespawnCircleDrawType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InteractHelper {
  private final Script script;

  public InteractHelper(Script script) {
    this.script = script;
  }

  public List<RSObject> getInteractiveObjects(String expectedName) {
    return script.getObjectManager().getObjects((RSObject object) -> {
      String name = object.getName();
      return name != null &&
        name.compareToIgnoreCase(expectedName) == 0 &&
        object.isInteractable();
    });
  }

  public List<RSObject> filterSpawnedObjects(List<RSObject> objects,
                                          RespawnCircleDrawType drawType,
                                          int zOffset,
                                          int distanceTolerance) {
    Map<RSObject, RespawnCircle> objectToRespawnCircleMap = script
      .getPixelAnalyzer()
      .getRespawnCircleObjects(objects, drawType, zOffset, distanceTolerance);
    Set<RSObject> unspawnedObjects = objectToRespawnCircleMap.keySet();

    return objects.stream().filter((object) -> !unspawnedObjects.contains(object)).toList();
  }

  public List<RSObject> filterUnsurroundedObjects(ScriptCore scriptCore, List<RSObject> objects) {
    List<LocalPosition> playerPositions = script
      .getWidgetManager()
      .getMinimap()
      .getPlayerPositions()
      .asList()
      .stream()
      .map((worldPosition) -> worldPosition.toLocalPosition(scriptCore))
      .toList();

    return objects.stream().filter((object) -> {
      for (LocalPosition surroundingPosition : object.getSurroundingPositions(1)) {
        if (playerPositions.contains(surroundingPosition)) {
          return false;
        }
      }
      return true;
    }).toList();
  }

  public boolean isPlayerIdling() {
    return !script.getPixelAnalyzer().isPlayerAnimating(0.25);
  }
}

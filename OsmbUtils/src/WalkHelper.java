import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.walker.WalkConfig;

import java.util.function.BooleanSupplier;

public class WalkHelper {
  private static final WalkConfig DEFAULT_WALK_CONFIG = new WalkConfig.Builder()
    .build();

  private final Script script;

  private final EntityHelper entityHelper;
  private final WaitHelper waitHelper;

  /**
   * @param script WalkHelper needs an instance of TaskLoopScript for an override to canHopWorlds
   */
  public WalkHelper(Script script) {
    this.script = script;

    entityHelper = new EntityHelper(script);
    waitHelper = new WaitHelper(script);
  }

  public boolean walkToAndInteract(String objectName, String menuItem) {
    RSObject object = script.getObjectManager().getClosestObject(objectName);
    if (object == null || !object.isInteractable()) {
      script.log(getClass(), "Failed to find object " + objectName);
      return false;
    }

    int initialDistance = object.getTileDistance();
    int destinationRadius = DEFAULT_WALK_CONFIG.getTileRandomisationRadius();
    if (!object.isInteractableOnScreen()) {
      if (!script.getWalker().walkTo(object, DEFAULT_WALK_CONFIG)) {
        script.log(getClass(), "Failed to start walking to object " + objectName);
        return false;
      }

      // TODO: Is this diagonal calculation needed?
      if (!waitForWalk(initialDistance, () -> object.distance() <= destinationRadius * 1.5)) {
        script.log(getClass(), "Failed to walk to object " + objectName);
        return false;
      }
    }

    if (!object.isInteractableOnScreen() || !object.interact(menuItem)) {
      script.log(getClass(), "Failed to interact with object " + objectName);
      return false;
    }

    return true;
  }

  public boolean walkToAndInteract(String objectName, String menuItem, WorldPosition approximatePosition) {
    int initialDistance = script.getWorldPosition().distanceTo(approximatePosition);
    int acceptableRadius = DEFAULT_WALK_CONFIG.getTileRandomisationRadius();

    if (initialDistance > acceptableRadius) {
      if (!script.getWalker().walkTo(approximatePosition, DEFAULT_WALK_CONFIG)) {
        script.log(getClass(), "Failed to start walking to approximate area " + objectName);
        return false;
      }

      if (!waitForWalk(
          initialDistance,
          () -> approximatePosition.distanceTo(script.getWorldPosition()) < acceptableRadius)) {
        script.log(getClass(), "Failed to walk to approximate area");
        return false;
      }
    }

    return walkToAndInteract(objectName, menuItem);
  }

  public boolean waitForWalk(int distance, BooleanSupplier earlyExitSupplier) {
    return waitHelper.waitForNoChange(
      "Walking",
      entityHelper::isPlayerIdling,
      1_500,
      distance * 1_000, // Should not take longer than 1s per tile to reach object
      earlyExitSupplier
    );
  }
}

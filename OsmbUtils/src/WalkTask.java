import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;

public class WalkTask extends Task {
  private final int acceptableInitialDistance;
  private final WorldPosition destinationPosition;
  private final EntityHelper entityHelper;
  private final WaitHelper waitHelper;

  /**
   * @param acceptableInitialDistance Task will not walk if within this distance to desired position. Keep in mind that
   *                                  the walker will not necessarily walk exactly to the desired position.
   */
  public WalkTask(Script script, WorldPosition destinationPosition, int acceptableInitialDistance) {
    super(script);
    this.acceptableInitialDistance = acceptableInitialDistance;
    this.destinationPosition = destinationPosition;

    entityHelper = new EntityHelper(script);
    waitHelper = new WaitHelper(script);
  }
  public WalkTask(Script script, WorldPosition destinationPosition) {
    this(script, destinationPosition, 0);
  }

  @Override
  public boolean canExecute() {
    return script.getWorldPosition().distanceTo(destinationPosition) > acceptableInitialDistance;
  }

  @Override
  public boolean execute() {
    int initialDistance = destinationPosition.distanceTo(destinationPosition);

    if (!script.getWalker().walkTo(destinationPosition)) {
      script.log(getClass(), "Failed to start walking to " + destinationPosition);
      return false;
    }

    return waitHelper.waitForNoChange(
      "Walking",
      entityHelper::isPlayerIdling,
      2_000, // Should not take more than 2s between run animations
      initialDistance * 1_000, // Should not take more than 1s per tile
      () -> script.getWorldPosition().distanceTo(destinationPosition) <= acceptableInitialDistance
    );
  }
}

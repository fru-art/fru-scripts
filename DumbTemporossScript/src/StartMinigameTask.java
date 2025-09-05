import com.osmb.api.location.position.types.WorldPosition;
import helper.DetectionHelper;
import helper.ObjectHelper;
import helper.WaitHelper;
import task.Task;

public class StartMinigameTask extends Task {
  private static final int BOAT_REGION = 12332;
  private static final int RUINS_REGION = 12588;
  private static final int MINIGAME_REGION = 12078;

  private final TemporossScript script;

  private final DetectionHelper detectionHelper;
  private final ObjectHelper objectHelper;
  private final WaitHelper waitHelper;

  public StartMinigameTask(TemporossScript script) {
    super(script);
    this.script = script;

    isCritical = true;
    retryLimit = 3;

    detectionHelper = new DetectionHelper(script);
    objectHelper = new ObjectHelper(script);
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    int region = script.getWorldPosition().getRegionID();

    if (region == MINIGAME_REGION && script.isMinigameOver()) {
      return true;
    }

    return region == BOAT_REGION || region == RUINS_REGION;
  }

  @Override
  public boolean execute() {
    int region = script.getWorldPosition().getRegionID();
    if (region == MINIGAME_REGION && script.isMinigameOver()) {
      script.log(getClass(), "Waiting for ferry back");
      script.submitHumanTask(
        () -> script.getWorldPosition().getRegionID() == RUINS_REGION || !script.isMinigameOver(), 20_000);
    }

    // Check if script needs to hop worlds or break, otherwise override for the duration of the minigame
    script.overrideCanBreak(null);
    script.overrideCanHopWorlds(null);
    script.submitTask(() -> false, 1);
    script.overrideCanBreak(false);
    script.overrideCanHopWorlds(false);

    region = script.getWorldPosition().getRegionID();
    if (region == RUINS_REGION) {
      script.log(getClass(), "Boarding boat");

      if (!objectHelper.walkToAndTap("Rope ladder", new WorldPosition(3137, 2840, 0), "Climb")) {
        script.log(getClass(), "Failed to walk to and tap rope ladder");
        return false;
      }

      waitHelper.waitForNoChange("position", script::getWorldPosition, 600, Integer.MAX_VALUE,
        () -> script.getWorldPosition().getRegionID() == BOAT_REGION);
      if (script.getWorldPosition().getRegionID() != BOAT_REGION) return false;
    }

    return script.submitHumanTask(() -> script.getWorldPosition().getRegionID() == MINIGAME_REGION, 60_000);
  }
}

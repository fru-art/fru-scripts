import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import helper.DetectionHelper;
import helper.ObjectHelper;
import helper.WaitHelper;
import task.Task;

public class StartMinigameTask extends Task {
  private static final int BOAT_REGION = 12332;
  private static final int RUINS_REGION = 12588;

  private final DumbTemporossScript script;

  private final ObjectHelper objectHelper;
  private final WaitHelper waitHelper;

  public StartMinigameTask(DumbTemporossScript script) {
    super(script);
    this.script = script;

    isCritical = true;
    retryLimit = 3;

    objectHelper = new ObjectHelper(script);
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    int region = getRegionId();

    if (region == DumbTemporossScript.TEMPOROSS_COVE_REGION && script.isMinigameOver()) {
      return true;
    }

    return region == BOAT_REGION || region == RUINS_REGION;
  }

  @Override
  public boolean execute() {
    int region = getRegionId();
    if (region == DumbTemporossScript.TEMPOROSS_COVE_REGION && script.isMinigameOver()) {
      script.log(getClass(), "Waiting for ferry back");
      script.submitHumanTask(
        () -> getRegionId() == RUINS_REGION || !script.isMinigameOver(), 20_000);
    }

    // Reset chatbox scroll position
    Chatbox chatbox = script.getWidgetManager().getChatbox();
    if (chatbox != null) {
      chatbox.openFilterTab(ChatboxFilterTab.PUBLIC);
      script.submitTask(() -> false, 1_000);
      chatbox.openFilterTab(ChatboxFilterTab.GAME);
    }

    // Check if script needs to hop worlds or break, otherwise override for the duration of the minigame
    script.overrideCanBreak(null);
    script.overrideCanHopWorlds(null);
    script.submitTask(() -> false, 1);
    script.overrideCanBreak(false);
    script.overrideCanHopWorlds(false);

    region = getRegionId();
    if (region == RUINS_REGION) {
      script.log(getClass(), "Boarding boat");

      if (!objectHelper.walkToAndTap("Rope ladder", new WorldPosition(3137, 2840, 0), "Climb")) {
        script.log(getClass(), "Failed to walk to and tap rope ladder");
        return false;
      }

      waitHelper.waitForNoChange("position", script::getWorldPosition, 600, Integer.MAX_VALUE,
        () -> getRegionId() == BOAT_REGION);
      if (getRegionId() != BOAT_REGION) return false;
    }

    return script.submitHumanTask(() -> getRegionId() == DumbTemporossScript.TEMPOROSS_COVE_REGION, 60_000);
  }

  private int getRegionId() {
    WorldPosition position = script.getWorldPosition();
    return position == null ? -1 : position.getRegionID();
  }
}

import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import task.Task;
import task.TaskScript;

public class StartMinigameTask extends Task {
  private static final RectangleArea INTERMEDIATE_BOAT = new RectangleArea(2638, 2642, 5, 5, 0);

  private final PestControlScript script;

  public StartMinigameTask(PestControlScript script) {
    super(script);
    this.script = script;

    isCritical = true;
    retryLimit = 2;
  }

  @Override
  public boolean canExecute() {
    return script.getWorldPosition().getRegionID() == PestControlScript.OUTPOST_REGION;
  }

  @Override
  public boolean execute() {
    if (!INTERMEDIATE_BOAT.contains(script.getWorldPosition())) {
      RSObject plank = script.getObjectManager().getRSObject(
        object -> object.getName() != null &&
          object.getName().contains("plank") &&
          INTERMEDIATE_BOAT.contains(object.getWorldPosition()));

      if (plank == null) return false;
      double initialDistance = plank.getTileDistance(script.getWorldPosition());
      if (!plank.interact("Cross")) return false;
      if (script.pollFramesHuman(
        () -> {
          WorldPosition position = script.getWorldPosition();
          return INTERMEDIATE_BOAT.contains(position) || position.getRegionID() == PestControlScript.MINIGAME_REGION;
        },
        (int) (initialDistance * 1_200 + 600))) return false;
      if (script.getWorldPosition().getRegionID() == PestControlScript.MINIGAME_REGION) return true;
    }

    script.resetChatboxScroll();
    return script.pollFramesHuman(
      () -> script.getWorldPosition().getRegionID() == PestControlScript.MINIGAME_REGION,
      60_000);
  }
}

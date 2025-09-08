import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import task.FirstMatchTaskScript;
import task.SequenceTaskScript;
import task.Task;

import java.util.List;

@ScriptDefinition(
  author = "fru",
  name = "Pest Control",
  description = "",
  skillCategory = SkillCategory.COMBAT,
  version = 0.1
)
public class PestControlScript extends FirstMatchTaskScript {
  public static final int MINIGAME_REGION = 10536;
  public static final RectangleArea NEAR_KNIGHT = new RectangleArea(2650, 2586, 13, 13, 0);
  public static final int OUTPOST_REGION = 10537;

  public PestControlScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of(
      new StartMinigameTask(this),
      new WalkToKnightTask(this),
      new KillPestsTask(this)
    );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(MINIGAME_REGION, OUTPOST_REGION);
  }

  public void resetChatboxScroll(boolean closeFirst) {
    Chatbox chatbox = this.getWidgetManager().getChatbox();
    if (chatbox == null) return;

    if (closeFirst && chatbox.isOpen()) {
      chatbox.close();
      this.pollFramesHuman(() -> !chatbox.isOpen(), 1_800);
    }

    if (!chatbox.isOpen()) {
      chatbox.open();
    } else {
      if (chatbox.getActiveFilterTab() == ChatboxFilterTab.GAME) {
        chatbox.openFilterTab(ChatboxFilterTab.PUBLIC);
        this.pollFramesHuman(() -> chatbox.getActiveFilterTab() == ChatboxFilterTab.PUBLIC, 1_800);
      }

      chatbox.openFilterTab(ChatboxFilterTab.GAME);
    }
  }
  public void resetChatboxScroll() {
    resetChatboxScroll(false);
  }
}

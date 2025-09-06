import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.utils.UIResultList;
import helper.DetectionHelper;
import task.Task;
import task.TaskScript;

public class ResetChatTask extends Task {
  private final DetectionHelper detectionHelper;

  public ResetChatTask(TaskScript script) {
    super(script);
    detectionHelper = new DetectionHelper(script);
  }

  @Override
  public boolean canExecute() {
    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    return chatboxText == null || chatboxText.isEmpty();
  }

  @Override
  public boolean execute() {
    Chatbox chatbox = script.getWidgetManager().getChatbox();
    if (chatbox == null) return false;

    if (chatbox.isOpen()) {
      chatbox.close();
      script.submitHumanTask(() -> !chatbox.isOpen(), 1_000);
    }

    chatbox.open();
    script.submitHumanTask(chatbox::isOpen, 1_000);
    return false;
  }
}

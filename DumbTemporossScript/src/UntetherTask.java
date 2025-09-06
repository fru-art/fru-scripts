import com.osmb.api.input.MenuEntry;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.utils.UIResultList;
import helper.DetectionHelper;
import helper.ObjectHelper;
import task.Task;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class UntetherTask extends Task {
  private final DumbTemporossScript script;

  private final DetectionHelper detectionHelper;
  private final ObjectHelper objectHelper;

  private boolean recentlyUntethered = false;

  public UntetherTask(DumbTemporossScript script) {
    super(script);
    isCritical = true;
    retryLimit = 3;

    this.script = script;
    this.detectionHelper = new DetectionHelper(script);
    this.objectHelper = new ObjectHelper(script);
  }

  @Override
  public boolean canExecute() {
    if (script.getIsland() == null) return false;

    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    if (chatboxText == null) return false;

    if (chatboxText.get(0).contains("untether yourself") && !recentlyUntethered) {
      return true;
    }

    recentlyUntethered = false;
    return false;
  }

  @Override
  public boolean execute() {
    // Reset chatbox scroll position because that's the most likely cause of tethering at the wrong time
    Chatbox chatbox = script.getWidgetManager().getChatbox();
    if (chatbox != null) {
      chatbox.close();
      script.submitTask(() -> false, 0);
      chatbox.open();
    }

    if (script.getIsland() == null) return false;

    WorldPosition position = script.getWorldPosition();

    List<RSObject> tetherables = objectHelper.getNamedObjects(List.of("Totem pole", "Mast"))
      .stream()
      .filter(object -> script.getIsland(object.getWorldPosition()) == script.getIsland())
      .sorted(Comparator.comparingInt(object -> object.getTileDistance(position)))
      .toList();

    if (tetherables.isEmpty()) {
      script.log(getClass(), "Failed to find tetherable");
      return false;
    }

    RSObject tetherable = tetherables.get(0);
    assert tetherable != null && tetherable.isInteractableOnScreen();

    AtomicBoolean alreadyUntethered = new AtomicBoolean(false);
    boolean result = tetherable.interact((menuEntries) -> {
      for (MenuEntry menuEntry : menuEntries) {
        String menuEntryText = menuEntry.getRawText().toLowerCase();
        if (menuEntryText.contains("untether")) return menuEntry;
        if (menuEntryText.contains("tether")) {
          alreadyUntethered.set(true);
          return null;
        }
      }

      return null;
    });

    if (alreadyUntethered.get() || result) {
      recentlyUntethered = true;
      return true;
    }

    return false;
  }
}

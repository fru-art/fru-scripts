import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.ui.hotkeys.Hotkeys;
import com.osmb.api.ui.tabs.Inventory;
import helper.InventoryHelper;
import task.Task;
import task.TaskScript;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CleanInventoryTask  extends Task {
  private static final Set<Integer> cleanables = Stream.concat(
    ArtefactCleanerScript.DROPS.stream(),
    ArtefactCleanerScript.INTERACTABLES.stream()
    ).collect(Collectors.toSet());

  private final InventoryHelper inventoryHelper;

  public CleanInventoryTask (TaskScript script) {
    super(script);

    inventoryHelper = new InventoryHelper(script, cleanables);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.containsAny(cleanables);
  }

  @Override
  public boolean execute() {
    Inventory inventory = inventoryHelper.getInventory();
    if (!inventory.dropItems(ArtefactCleanerScript.DROPS)) {
      script.log(getClass(), "Failed to clean drops");
    }

    Hotkeys hotkeys = script.getWidgetManager().getHotkeys();
    if (hotkeys != null && hotkeys.isTapToDropEnabled().getIfFound()) {
      hotkeys.setTapToDropEnabled(false);
    }

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    snapshot.getAllOfItems(ArtefactCleanerScript.INTERACTABLES).forEach(interactables -> {
      if (!interactables.interact()) {
        script.log(getClass(), "Failed to clean interactables");
      }
    });

    return script.pollFramesHuman(() -> !inventoryHelper.getSnapshot().containsAny(cleanables), 2 * 600);
  }
}

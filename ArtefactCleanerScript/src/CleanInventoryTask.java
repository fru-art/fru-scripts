import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
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
    ArtefactCleanerScript.INTERACTABLES.stream())
    .collect(Collectors.toSet());

  private final InventoryHelper inventoryHelper;

  public CleanInventoryTask (TaskScript script) {
    super(script);

    inventoryHelper = new InventoryHelper(script, ArtefactCleanerScript.allItems);
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
    // Needed for dropping unrecognized items such as weird bowl
    if (hotkeys != null) {
      ItemGroupResult snapshot = inventoryHelper.getSnapshot();
      Set<Integer> occupiedSlots = snapshot.getOccupiedSlots();
      Set<Integer> recognizedSlots = snapshot.getRecognisedItems().stream()
        .map(ItemSearchResult::getSlot)
        .collect(Collectors.toSet());
      Set<Integer> unrecognizedSlots = occupiedSlots.stream()
        .filter(slot -> !recognizedSlots.contains(slot))
        .collect(Collectors.toSet());

      if (!unrecognizedSlots.isEmpty()) {
        hotkeys.setTapToDropEnabled(true);

        for (Integer slot: unrecognizedSlots) {
          Rectangle slotBounds = inventory.getBoundsForSlot(slot).get();
          script.getFinger().tap(slotBounds);
        }
      }
    }

    if (!ArtefactCleanerScript.INTERACTABLES.isEmpty()) {
      if (hotkeys != null && hotkeys.isTapToDropEnabled().getIfFound()) {
        hotkeys.setTapToDropEnabled(false);
        script.log(getClass(), "Disabled tap to drop");
      }

      ItemGroupResult snapshot = inventoryHelper.getSnapshot();
      snapshot.getAllOfItems(ArtefactCleanerScript.INTERACTABLES).forEach(interactables -> {
        if (!interactables.interact()) {
          script.log(getClass(), "Failed to clean interactables");
        }
      });
    }

    return script.pollFramesHuman(
      () -> !inventoryHelper.getSnapshot().containsAny(cleanables), 2 * 600, true);
  }
}

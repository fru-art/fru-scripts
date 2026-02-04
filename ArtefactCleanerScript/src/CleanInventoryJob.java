import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.hotkeys.Hotkeys;
import com.osmb.api.ui.tabs.Inventory;
import com.osmbtoolkit.job.Job;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class CleanInventoryJob extends Job<ArtefactCleanerScript> {
  private static final Set<Integer> cleanables =
    Stream.concat(ArtefactCleanerScript.DROPS.stream(), ArtefactCleanerScript.INTERACTABLES.stream())
      .collect(Collectors.toSet());

  public CleanInventoryJob(ArtefactCleanerScript script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventory(cleanables);
    return snapshot.containsAny(cleanables);
  }

  @Override
  public boolean execute() {
    ItemGroupResult snapshot;
    Hotkeys hotkeys;
    WidgetManager widgetManager = script.getWidgetManager();
    if (widgetManager == null) return false;
    Inventory inventory = script.getWidgetManager().getInventory();
    if (inventory == null) return false;

    if (!inventory.dropItems(ArtefactCleanerScript.DROPS)) {
      this.script.log(this.getClass(), "Failed to clean drops");
    }
    if ((hotkeys = this.script.getWidgetManager().getHotkeys()) != null) {
      snapshot = script.pollFramesUntilInventory(ArtefactCleanerScript.allItems);
      Set<Integer> occupiedSlots = snapshot.getOccupiedSlots();
      Set<Integer> recognizedSlots =
        snapshot.getRecognisedItems().stream().map(ItemSearchResult::getSlot).collect(Collectors.toSet());
      Set<Integer> unrecognizedSlots =
        occupiedSlots.stream().filter(slot -> !recognizedSlots.contains(slot)).collect(Collectors.toSet());
      if (!unrecognizedSlots.isEmpty()) {
        hotkeys.setTapToDropEnabled(true);
        for (Integer slot2 : unrecognizedSlots) {
          Rectangle slotBounds = (Rectangle) inventory.getBoundsForSlot(slot2).get();
          this.script.getFinger().tap((Shape) slotBounds);
        }
      }
    }
    if (!ArtefactCleanerScript.INTERACTABLES.isEmpty()) {
      if (hotkeys != null && (Boolean) hotkeys.isTapToDropEnabled().getIfFound()) {
        hotkeys.setTapToDropEnabled(false);
        this.script.log(this.getClass(), "Disabled tap to drop");
      }
      snapshot = script.pollFramesUntilInventory(ArtefactCleanerScript.allItems);
      snapshot.getAllOfItems(ArtefactCleanerScript.INTERACTABLES).forEach(interactables -> {
        if (!interactables.interact()) {
          this.script.log(this.getClass(), "Failed to clean interactables");
        }
      });
    }

    return this.script.pollFramesHuman(
      () -> !script.pollFramesUntilInventory(cleanables).containsAny(cleanables),
      1200,
      true);
  }
}

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.hotkeys.Hotkeys;
import com.osmb.api.ui.tabs.Inventory;
import helper.InventoryHelper;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import task.Task;
import task.TaskScript;

public class CleanInventoryTask
extends Task {
    private static final Set<Integer> cleanables = Stream.concat(ArtefactCleanerScript.DROPS.stream(), ArtefactCleanerScript.INTERACTABLES.stream()).collect(Collectors.toSet());
    private final InventoryHelper inventoryHelper;

    public CleanInventoryTask(TaskScript script) {
        super(script);
        this.inventoryHelper = new InventoryHelper((Script)script, ArtefactCleanerScript.allItems);
    }

    @Override
    public boolean canExecute() {
        ItemGroupResult snapshot = this.inventoryHelper.getSnapshot();
        return snapshot.containsAny(cleanables);
    }

    @Override
    public boolean execute() {
        ItemGroupResult snapshot;
        Hotkeys hotkeys;
        Inventory inventory = this.inventoryHelper.getInventory();
        if (!inventory.dropItems(ArtefactCleanerScript.DROPS)) {
            this.script.log(this.getClass(), "Failed to clean drops");
        }
        if ((hotkeys = this.script.getWidgetManager().getHotkeys()) != null) {
            snapshot = this.inventoryHelper.getSnapshot();
            Set<Integer> occupiedSlots = snapshot.getOccupiedSlots();
            Set<Integer> recognizedSlots = snapshot.getRecognisedItems().stream().map(ItemSearchResult::getSlot).collect(Collectors.toSet());
            Set<Integer> unrecognizedSlots = occupiedSlots.stream().filter(slot -> !recognizedSlots.contains(slot)).collect(Collectors.toSet());
            if (!unrecognizedSlots.isEmpty()) {
                hotkeys.setTapToDropEnabled(true);
                for (Integer slot2 : unrecognizedSlots) {
                    Rectangle slotBounds = (Rectangle)inventory.getBoundsForSlot(slot2.intValue()).get();
                    this.script.getFinger().tap((Shape)slotBounds);
                }
            }
        }
        if (!ArtefactCleanerScript.INTERACTABLES.isEmpty()) {
            if (hotkeys != null && ((Boolean)hotkeys.isTapToDropEnabled().getIfFound()).booleanValue()) {
                hotkeys.setTapToDropEnabled(false);
                this.script.log(this.getClass(), "Disabled tap to drop");
            }
            snapshot = this.inventoryHelper.getSnapshot();
            snapshot.getAllOfItems(ArtefactCleanerScript.INTERACTABLES).forEach(interactables -> {
                if (!interactables.interact()) {
                    this.script.log(this.getClass(), "Failed to clean interactables");
                }
            });
        }
        return this.script.pollFramesHuman(() -> !this.inventoryHelper.getSnapshot().containsAny(cleanables), 1200, true);
    }
}

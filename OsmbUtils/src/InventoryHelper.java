import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Inventory;

import java.util.Set;

public class InventoryHelper {
  private final Set<Integer> recognizedInventoryItemIds;
  private final Script script;

  public InventoryHelper(Script script, Set<Integer> recognizedInventoryItemIds) {
    this.recognizedInventoryItemIds = recognizedInventoryItemIds;
    this.script = script;
  }

  /**
   * @return Item group result for the recognized item ids provided during instantiation. Don't forget that this
   *         object's methods (such as containment checkers) also depend on the original recognized item ids.
   */
  public ItemGroupResult getSnapshot(boolean skipHumanDelay) {
    Inventory inventory = script.getWidgetManager().getInventory();
    if (!inventory.isOpen()) {
      inventory.open();

      if (skipHumanDelay) {
        script.submitTask(() -> inventory.isVisible() && inventory.isOpen(), 3_000);
      } else {
        script.submitHumanTask(() -> inventory.isVisible() && inventory.isOpen(), 3_000);
      }
    }

    return inventory.search(recognizedInventoryItemIds);
  }

  public ItemGroupResult getSnapshot() {
    return getSnapshot(false);
  }
}

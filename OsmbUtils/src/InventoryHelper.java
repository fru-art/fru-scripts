import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;

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
  public ItemGroupResult getSnapshot() {
    return script.getWidgetManager().getInventory().search(recognizedInventoryItemIds);
  }
}

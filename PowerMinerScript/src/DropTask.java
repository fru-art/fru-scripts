import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;

import java.util.Set;

public class DropTask extends Task {
  private final Set<Integer> droppables;
  private final InventoryHelper inventoryHelper;

  public DropTask(Script script, PowerMinerScriptOptions scriptOptions) {
    super(script);

    droppables = scriptOptions.mineables;
    inventoryHelper = new InventoryHelper(script, droppables);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();
    return inventorySnapshot.isFull() && inventorySnapshot.containsAny(droppables);
  }

  @Override
  public boolean execute() {
    return script.getWidgetManager().getInventory().dropItems(droppables);
  }
}

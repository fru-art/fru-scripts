import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;

import java.util.Set;

public class DropTask extends Task {
  private final Integer dropThreshold;
  private final Set<Integer> droppables;
  private final InventoryHelper inventoryHelper;

  /**
   * @param script
   * @param droppables
   * @param dropThreshold Having more than this amount of droppables is a requirement to execute. Leave as null if you
   *                      want to drop the items no matter the count.
   */
  public DropTask(Script script, Set<Integer> droppables, Integer dropThreshold) {
    super(script);

    this.droppables = droppables;
    this.dropThreshold = dropThreshold;

    inventoryHelper = new InventoryHelper(script, droppables);
  }
  public DropTask(Script script, Set<Integer> droppables) {
    this(script, droppables, null);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();

    if (dropThreshold == null) {
      return inventorySnapshot.containsAny(droppables);
    }

    return inventorySnapshot.getAmount(droppables) >= dropThreshold;
  }

  @Override
  public boolean execute() {
    return script.getWidgetManager().getInventory().dropItems(droppables);
  }
}

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.bank.Bank;
import helper.InventoryHelper;
import task.Task;
import task.TaskScript;

import java.util.Set;

public class OpenAllCoinPouchTask extends Task {
  private final InventoryHelper inventoryHelper;

  public OpenAllCoinPouchTask(TaskScript script) {
    super(script);
    inventoryHelper = new InventoryHelper(script, Set.of(ItemID.COIN_POUCH));
  }

  @Override
  public boolean canExecute() {
    Bank bank = script.getWidgetManager().getBank();
    // Can't click in bank UI
    if (bank != null && bank.isVisible()) {
      script.log("Skipping opening coin pouch due to in bank interface");
      return false;
    }

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    if (snapshot == null) {
      script.log("Skipping opening coin pouch due to cant find inventory");
      return false;
    }
    return snapshot.contains(ItemID.COIN_POUCH);
  }

  @Override
  public boolean execute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    if (!snapshot.contains(ItemID.COIN_POUCH)) return true;

    snapshot.getItem(ItemID.COIN_POUCH).interact();
    return script.pollFramesHuman(
      () -> !inventoryHelper.getSnapshot().contains(ItemID.COIN_POUCH), 1_800, true);
  }
}

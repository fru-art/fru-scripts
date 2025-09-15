import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;
import task.TaskScript;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CleanFindsTask extends Task {
  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;

  public CleanFindsTask(TaskScript script) {
    super(script);

    inventoryHelper = new InventoryHelper(script, Stream.concat(
      ArtefactCleanerScript.TOOLS.stream(),
      Stream.of(ItemID.UNCLEANED_FIND)
    ).collect(Collectors.toSet()));
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();

    return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && snapshot.contains(ItemID.UNCLEANED_FIND);
  }

  @Override
  public boolean execute() {
    RSObject table = script.getObjectManager().getClosestObject("Specimen table");
    if (table == null) {
      script.log(getClass(), "Failed to find table");
      return false;
    }

    int initialDistance = table.getTileDistance(script.getWorldPosition());

    if (!table.interact("Clean")) {
      script.log(getClass(), "Failed to interact with table");
      return false;
    }

    waitHelper.waitForNoChange("position",
      script::getWorldPosition,
      600,
      initialDistance * 600 + 600);

    waitHelper.waitForNoChange("find-count",
      () -> inventoryHelper.getSnapshot().getAmount(ItemID.UNCLEANED_FIND),
      10 * 600,
      Integer.MAX_VALUE,
      () -> !inventoryHelper.getSnapshot().contains(ItemID.UNCLEANED_FIND));

    return !inventoryHelper.getSnapshot().contains(ItemID.UNCLEANED_FIND);
  }

}

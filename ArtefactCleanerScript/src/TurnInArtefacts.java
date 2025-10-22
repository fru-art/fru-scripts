import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;
import task.TaskScript;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TurnInArtefacts extends Task {

  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;

  public TurnInArtefacts(TaskScript script) {
    super(script);

    inventoryHelper = new InventoryHelper(
      script,
      Stream.concat(
        ArtefactCleanerScript.ARTEFACTS.stream(),
        Stream.of(ItemID.ANTIQUE_LAMP))
        .collect(Collectors.toSet())
    );
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.containsAny(ArtefactCleanerScript.ARTEFACTS) && !snapshot.containsAny(ItemID.ANTIQUE_LAMP);
  }

  @Override
  public boolean execute() {
    RSObject crate = script.getObjectManager().getClosestObject(script.getWorldPosition(), "Storage crate");
    if (crate == null) {
      script.log(getClass(), "Failed to find crate");
      return false;
    }

    int initialDistance = crate.getTileDistance(script.getWorldPosition());

    if (!crate.interact("Add finds")) {
      script.log(getClass(), "Failed to interact with crate");
      return false;
    }

    waitHelper.waitForNoChange("position",
      script::getWorldPosition,
      600,
      initialDistance * 600 + 600);

    waitHelper.waitForNoChange("artefact-count",
      () -> inventoryHelper.getSnapshot().getAmount(ArtefactCleanerScript.ARTEFACTS),
      6 * 600,
      Integer.MAX_VALUE,
      () -> !inventoryHelper.getSnapshot().containsAny(ArtefactCleanerScript.ARTEFACTS));

    return inventoryHelper.getSnapshot().containsAny(ArtefactCleanerScript.ARTEFACTS);
  }

}

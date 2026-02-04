import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmbtoolkit.job.Job;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class CleanFindsJob extends Job<ArtefactCleanerScript> {
  Set<Integer> items =
    Stream.concat(ArtefactCleanerScript.TOOLS.stream(), Stream.of(ItemID.UNCLEANED_FIND)).collect(Collectors.toSet());

  public CleanFindsJob(ArtefactCleanerScript script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventory(items);
    return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && snapshot.contains(ItemID.UNCLEANED_FIND) && snapshot.isFull();
  }

  @Override
  public boolean execute() {
    RSObject table =
      this.script.getObjectManager().getClosestObject(this.script.getWorldPosition(), new String[]{"Specimen table"});
    if (table == null) {
      this.script.log(this.getClass(), "Failed to find table");
      return false;
    }
    int initialDistance = table.getTileDistance(this.script.getWorldPosition());
    if (!table.interact(new String[]{"Clean"})) {
      this.script.log(this.getClass(), "Failed to interact with table");
      return false;
    }
    script.pollFramesUntilStill();
    script.pollFramesUntilNoChange(
      () -> script.pollFramesUntilInventory(items).getAmount(ItemID.UNCLEANED_FIND),
      6_000,
      Integer.MAX_VALUE,
      () -> !script.pollFramesUntilInventory(items).contains(ItemID.UNCLEANED_FIND));
    return !script.pollFramesUntilInventory(items).contains(ItemID.UNCLEANED_FIND);
  }
}

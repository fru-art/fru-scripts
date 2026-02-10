import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.scene.RSObject;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class TurnInArtefactsJob extends Job<ToolkitScript> {
  private Set<Integer> items =
    Stream.concat(ArtefactCleanerScript.ARTEFACTS.stream(), Stream.of(4447, 11175)).collect(Collectors.toSet());

  public TurnInArtefactsJob(ToolkitScript script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventoryVisible(items);
    return snapshot.containsAny(ArtefactCleanerScript.ARTEFACTS) && !snapshot.contains(11175) && !snapshot.containsAny(
      4447);
  }

  @Override
  public boolean execute() {
    RSObject crate =
      this.script.getObjectManager().getClosestObject(this.script.getWorldPosition(), "Storage crate");
    if (crate == null) {
      this.script.log(this.getClass(), "Failed to find crate");
      return false;
    }
    if (!crate.interact("Add finds")) {
      this.script.log(this.getClass(), "Failed to interact with crate");
      return false;
    }
    script.pollFramesUntilNoMovement();
    script.pollFramesUntilNoChange(() -> script.pollFramesUntilInventoryVisible(items).getAmount(ArtefactCleanerScript.ARTEFACTS), 3_600);
    return !script.pollFramesUntilInventoryVisible(items).containsAny(ArtefactCleanerScript.ARTEFACTS);
  }
}

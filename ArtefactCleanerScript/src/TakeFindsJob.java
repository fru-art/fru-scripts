import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.scene.RSObject;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class TakeFindsJob extends Job<ToolkitScript> {
  private final Set<Integer> items = Stream.concat(ArtefactCleanerScript.TOOLS.stream(), Stream.of(11175)).collect(
    Collectors.toSet());
  private final ArtefactCleanerScript script;

  private final Random random = new Random();

  public TakeFindsJob(ArtefactCleanerScript script) {
    super(script);
    this.script = script;
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventoryVisible(items);
    return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && !snapshot.isFull();
  }

  @Override
  public boolean execute() {
    RSObject rocks = this.script.getObjectManager()
      .getClosestObject(this.script.getWorldPosition(), new String[]{"Dig Site specimen rocks"});
    if (rocks == null) {
      this.script.log(this.getClass(), "Failed to find rocks");
      return false;
    }
    if (!rocks.interact("Take")) {
      this.script.log(this.getClass(), "Failed to interact with rocks");
      return false;
    }
    script.pollFramesUntilNoMovement();
    script.pollFramesUntilNoChange(
      () -> script.pollFramesUntilInventoryVisible(items).getAmount(11175),
      3_000,
      Integer.MAX_VALUE,
      () -> script.pollFramesUntilInventoryVisible(items).isFull());

    return script.pollFramesUntilInventoryVisible(items).isFull();
  }
}

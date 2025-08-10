import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.List;

@ScriptDefinition(
  author = "fru",
  name = "Camdozaal Fisher",
  description = "Net fishing and offering in Ruins of Camdozaal",
  skillCategory = SkillCategory.PRAYER, version = 1.1
)

// Please do not use the high-level design of this script as an example for your own code; it's bad. This is my first
// OSMB script and first time coding in Java in a while. Only reference the low-level OSMB API usage if needed.
public class CamdozaalFisher extends Script {

  private static final int CAMDOZAAL_REGION_ID = 11610;

  // List of available tasks ordered by priority to execute
  private final List<Task> tasks = List.copyOf(List.of(
    new PrepareTask(this),
    new OfferTask(this),
    new DropTask(this),
    new FishTask(this)
  ));
  private Long lastExecutionEpoch;

  public CamdozaalFisher(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  public int poll() {
    if (getWorldPosition().getRegionID() != CAMDOZAAL_REGION_ID) {
      log(getClass(), "Exiting script due to not near Camdozaal fishing area");
      stop();
    }

    // On each poll, run the first executable task from the list
    for (Task task : tasks) {
      if (task.canExecute()) {
        if (!task.execute()) {
          log(getClass(), "Failed to execute " + task.getClass());
          return 1000;
        }

        lastExecutionEpoch = System.currentTimeMillis();
        return 0;
      }
    }

    // Exit script if no successful task executions in the last minute
    if (lastExecutionEpoch != null && System.currentTimeMillis() - lastExecutionEpoch > 60_000) {
      log(getClass(), "Exiting script due to long period of inactivity");
      stop();
    }

    log(getClass(), "No executable task found. Re-polling");
    return 0;
  }

  @Override
  public int[] regionsToPrioritise() {
    return new int[]{CAMDOZAAL_REGION_ID};
  }
}

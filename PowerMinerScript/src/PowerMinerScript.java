import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Power Miner",
  description = "Mines and drops up to iron",
  skillCategory = SkillCategory.MINING,
  version = 1.0
)

// TODO: Migrate to extend TaskLoopScript instead when OSMB updates to be compatible with Script superclasses
public class PowerMinerScript extends Script {
  public static final Set<Integer> GEMS = Set.of(
    ItemID.UNCUT_SAPPHIRE,
    ItemID.UNCUT_RUBY,
    ItemID.UNCUT_EMERALD,
    ItemID.UNCUT_DIAMOND
  );

  // Needed to convert between position types
  public final ScriptCore scriptCore;

  private final List<Integer> requiredRegions;
  private final PowerMinerScriptOptions scriptOptions;
  private final long scriptTimeoutMs;
  private List<Task> taskList;
  private Long lastExecutionTime;

  public PowerMinerScript(Object scriptCore) {
    super(scriptCore);
    log("asdf");
    this.scriptCore = (ScriptCore) scriptCore;
    requiredRegions = getRequiredRegions();
    scriptOptions = new PowerMinerScriptOptions(this);
    scriptTimeoutMs = getScriptTimeoutMs();
  }

  @Override
  public void onStart() {
    log(getClass(), "onStart()");
    scriptOptions.show();
  }

  @Override
  public int poll() {
    // Do this here to ensure tasks have access to accurate script options during instantiation
    if (taskList == null) taskList = getTaskList();

    int currentRegion = getWorldPosition().getRegionID();
    if (!requiredRegions.contains(currentRegion)) {
      log(getClass(), "Exiting script due to player not required region. Current region: " + currentRegion);
      stop();
      return 1000;
    }

    if (lastExecutionTime != null && System.currentTimeMillis() - lastExecutionTime > scriptTimeoutMs) {
      log(getClass(), "Exiting script due to long period of inactivity");
      stop();
      return 1000;
    }

    for (Task task : taskList) {
      if (task.canExecute()) {
        log(getClass(), "Executing " + task.getClass());
        if (!task.execute()) {
          log(getClass(), "Failed to execute " + task.getClass());
          return 1000;
        }

        lastExecutionTime = System.currentTimeMillis();
        return 0;
      }
    }

    log(getClass(), "Could not find executable task, polling again");
    return 0;
  }

  /**
   * Provide the script timeout duration for when a task hasn't successfully executed in a while. The default timeout is
   * 60s if not overridden.
   *
   * @return Timeout duration in milliseconds, can be null if script should run forever
   */
  protected Long getScriptTimeoutMs() {
    return 120_000L;
  };

  @Override
  public int[] regionsToPrioritise() {
    return requiredRegions.stream()
      .filter(Objects::nonNull)
      .mapToInt(Integer::intValue)
      .toArray();
  }

  // The following belongs in SimplePowerMinerScript even post-migration to TaskLoopScript.
  protected List<Task> getTaskList() {
    assert scriptOptions != null;
    return List.of(
      new DropTask(this, scriptOptions),
      new MineTask(this, scriptOptions)
    );
  };
  protected List<Integer> getRequiredRegions() {
    return List.of(
      8747, // Isle of Souls
      10804, 10803, // Legends' Guild
      11826, // Rimmington
      12183, 12184, 12185, // Mining Guild
      13107, // Al Kharid
      13108 // Southeast Varrock
    );
  }
}


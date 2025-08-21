import com.osmb.api.script.Script;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public abstract class TaskScript extends Script {
  protected Long lastExecutionTime;
  protected List<Task> taskList;
  private final List<Integer> requiredRegions;
  private final long scriptTimeoutMs;

  public TaskScript(Object scriptCore) {
    super(scriptCore);
    requiredRegions = getRequiredRegions();
    scriptTimeoutMs = getScriptTimeoutMs();
  }

  @Override
  public int poll() {
    // Do not do this during construction in case script needs to query options from the user first
    if (taskList == null) taskList = getTaskList();

    int currentRegion = getWorldPosition().getRegionID();
    if (!requiredRegions.isEmpty() && !requiredRegions.contains(currentRegion)) {
      log(getClass(), "Exiting script due to player not in required region. Current region: " + currentRegion);
      stop();
      return 1000;
    }

    if (lastExecutionTime != null && System.currentTimeMillis() - lastExecutionTime > scriptTimeoutMs) {
      log(getClass(), "Exiting script due to long period of inactivity");
      stop();
      return 1000;
    }

    return executeTasks();
  }

  protected abstract int executeTasks();

  /**
   * Provide the script timeout duration for when a task hasn't successfully executed in a while. The default timeout is
   * 5m if not overridden.
   *
   * @return Timeout duration in milliseconds, can be null if script should run forever
   */
  protected Long getScriptTimeoutMs() {
    return 300_000L;
  };

  protected abstract List<Task> getTaskList();

  protected List<Integer> getRequiredRegions() {
    return Collections.emptyList();
  };

  protected List<Integer> getRegionsToPrioritize() {
    return Collections.emptyList();
  };

  @Override
  public int[] regionsToPrioritise() {
    return Stream.concat(requiredRegions.stream(), getRegionsToPrioritize().stream())
      .distinct()
      .mapToInt(Integer::intValue)
      .toArray();
  }
}

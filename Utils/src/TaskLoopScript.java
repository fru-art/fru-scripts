import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class TaskLoopScript extends Script {
  private final List<Integer> requiredRegions;
  private final long scriptTimeoutMs;
  private List<Task> taskList;
  private Long lastExecutionTime;

  public TaskLoopScript(Object scriptCore) {
    super(scriptCore);
    requiredRegions = getRequiredRegions();
    scriptTimeoutMs = getScriptTimeoutMs();
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
    return 60_000L;
  };

  protected abstract List<Task> getTaskList();
  protected abstract List<Integer> getRequiredRegions();

  @Override
  public int[] regionsToPrioritise() {
    return requiredRegions.stream()
      .filter(Objects::nonNull)
      .mapToInt(Integer::intValue)
      .toArray();
  }
}

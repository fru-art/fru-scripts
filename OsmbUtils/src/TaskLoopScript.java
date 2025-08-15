import com.osmb.api.script.Script;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Executes tasks based on order in task list, highest priority tasks can loop until their pre-reqs are not met.
 */
public abstract class TaskLoopScript extends Script {
  private final List<Integer> requiredRegions;
  private final long scriptTimeoutMs;
  private Long lastExecutionTime;
  private List<Task> taskList;

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

  protected int executeTasks() {
    for (Task task : taskList) {
      do {
        if (task.canExecute()) {
          log(getClass(), "Executing " + task.getClass());
          if (!task.execute()) {
            log(getClass(), "Failed to execute " + task.getClass());
            return 1000;
          }

          lastExecutionTime = System.currentTimeMillis();
          return 0;
        }
      } while (task.canLoop && task.canExecute());
    }

    log(getClass(), "Could not find executable task, polling again");
    return 0;
  }

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

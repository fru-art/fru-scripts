/**
 * @deprecated
 */
public abstract class FirstMatchTaskScript extends TaskScript {
  public FirstMatchTaskScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
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
        }
      } while (task.canLoop && task.canExecute());

      return 0;
    }

    log(getClass(), "Could not find executable task, polling again");
    return 0;
  }
}

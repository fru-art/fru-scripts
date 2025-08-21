public abstract class SequenceTaskScript extends TaskScript {
  public SequenceTaskScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected int executeTasks() {
    for (Task task : taskList) {
      do {
        if (task.canExecute()) {
          log(getClass(), "Executing " + task.getClass());
          if (!task.execute()) {
            if (task.isCritical) {
              log(getClass(), "Failed to execute critical " + task.getClass());
              this.stop();
              return 1000;
            }
            log(getClass(), "Failed to execute " + task.getClass());
            break;
          }

          lastExecutionTime = System.currentTimeMillis();
        }
      } while (task.canLoop && task.canExecute());
    }

    log(getClass(), "Completed all executable tasks, polling again");
    return 0;
  }
}

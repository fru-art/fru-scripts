import com.osmb.api.script.Script;

public abstract class Task {
  protected final Script script;

  public boolean canLoop = false;

  public Task(Script script) {
    this.script = script;
  }

  public abstract boolean canExecute();

  public abstract boolean execute();

  public Task setCanLoop(boolean canLoop) {
    this.canLoop = canLoop;
    return this;
  }
}

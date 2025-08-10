import com.osmb.api.script.Script;

public abstract class Task {
  protected final Script script;

  public Task(Script script) {
    this.script = script;
  }

  public abstract boolean canExecute();
  public abstract boolean execute();
}

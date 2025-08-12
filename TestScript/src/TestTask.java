import com.osmb.api.script.Script;

public class TestTask extends Task {
  public TestTask(Script script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    return true;
  }

  @Override
  public boolean execute() {
    script.log(getClass(), "Test message");
    return true;
  }
}

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.List;

@ScriptDefinition(
  author = "fru",
  name = "Test",
  description = "A test script",
  skillCategory = SkillCategory.OTHER,
  version = 0.1
)

public class TestScript extends TaskLoopScript {
  public TestScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of();
  }

  @Override
  protected List<Integer> getRegionsToPrioritize() {
    return List.of(
      11610, // Camdozaal resources
      12596,
      12598, // Grand Exchange
      12849);
  }
}

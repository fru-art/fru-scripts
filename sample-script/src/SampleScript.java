import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.Collections;

@ScriptDefinition(
  author = "fru",
  name = "Sample",
  description = "Sample script",
  skillCategory = SkillCategory.COMBAT, version = 0.1
)

public class SampleScript extends Script {
  public SampleScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  public int poll() {
    log(SampleScript.class, "Inventory count: " +
      getWidgetManager().getInventory().search(Collections.emptySet()).getAmount(Collections.emptySet()));

    return 0;
  }
}

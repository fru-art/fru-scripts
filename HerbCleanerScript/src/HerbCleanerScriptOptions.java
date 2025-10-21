import com.osmb.api.script.Script;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;

import java.util.List;

public class HerbCleanerScriptOptions extends ScriptOptions {
  public final CheckBox fastCleanCheckBox;

  public HerbCleanerScriptOptions(Script script) {
    super(script);
    fastCleanCheckBox = getCheckBox("Fast clean (Not recommended for prolonged runtime)", false);
  }

  @Override
  public List<Node> getRootChildren() {
    return List.of(
      new Label("Remember: Inventory shouldn't have any other items"),
      fastCleanCheckBox
    );
  }
}

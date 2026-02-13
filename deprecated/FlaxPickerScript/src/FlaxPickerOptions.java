import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

import java.util.List;

public class FlaxPickerOptions extends Options {
  public final CheckBox debugCheckBox;

  public FlaxPickerOptions(ToolkitScript script) {
    super(script);
    debugCheckBox = getCheckBox("Draw debug", true);
  }

  @Override
  public List<Node> getChildren() {
    return List.of(debugCheckBox);
  }
}

import com.osmb.api.script.Script;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.util.List;

public class BeginnerStallThieverScriptOptions extends ScriptOptions {
  public final RadioButton bakersStallRadioButton;
  public final RadioButton fruitStallRadioButton;

  public BeginnerStallThieverScriptOptions(Script script) {
    super(script);

    ToggleGroup toggleGroup = new ToggleGroup();
    bakersStallRadioButton = getRadioButton("Baker's stall (start in Ardougne market)", true, toggleGroup);
    fruitStallRadioButton = getRadioButton("Fruit stall (start in Hosidius house)", false, toggleGroup);
  }

  @Override
  public List<Node> getRootChildren() {
    return List.of(
      bakersStallRadioButton,
      fruitStallRadioButton
    );
  }
}

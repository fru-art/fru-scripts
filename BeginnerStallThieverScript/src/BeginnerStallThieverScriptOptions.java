import com.osmb.api.script.Script;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;

import java.util.List;

public class BeginnerStallThieverScriptOptions extends ScriptOptions {
  public final CheckBox bankCheckbox;

  public final RadioButton bakersStallRadioButton;
  public final RadioButton fruitStallRadioButton;
  public final RadioButton teaStallRadioButton;

  public BeginnerStallThieverScriptOptions(Script script) {
    super(script);

    bankCheckbox = getCheckBox("Bank (does not work with fruit stall)", false);

    ToggleGroup toggleGroup = new ToggleGroup();
    bakersStallRadioButton = getRadioButton("Baker's stall (start in Ardougne market)", true, toggleGroup);
    fruitStallRadioButton = getRadioButton("Fruit stall (start in Hosidius house)", false, toggleGroup);
    teaStallRadioButton = getRadioButton("Tea stall (start in southeast Varrock)", false, toggleGroup);
  }

  @Override
  public List<Node> getRootChildren() {
    return List.of(
      bakersStallRadioButton,
      teaStallRadioButton,
      fruitStallRadioButton,
      new Separator(),
      bankCheckbox
    );
  }
}

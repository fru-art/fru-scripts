import com.osmb.api.script.Script;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;

import java.util.List;

public class BeginnerPickpocketerScriptOptions extends ScriptOptions {
  public final CheckBox eatCheckBox;
  public final RadioButton guardEdgevilleRadioButton;
  public final RadioButton guardVarrockRadioButton;
  public final RadioButton manRadioButton;
  public final RadioButton warriorRadioButton;

  private final ToggleGroup radioButtonGroup;

  public BeginnerPickpocketerScriptOptions(Script script) {
    super(script);

    radioButtonGroup = new ToggleGroup();

    eatCheckBox = getCheckBox("Withdraw and eat", true);
    guardEdgevilleRadioButton = getRadioButton("Guard, Edgeville", false);
    guardVarrockRadioButton = getRadioButton("Guard, Varrock", false);
    manRadioButton = getRadioButton("Man, Edgeville", true);
    warriorRadioButton = getRadioButton("Warrior, Al-Kharid (not working)", false);
    warriorRadioButton.setDisable(true); // TODO: Update when fixed
  }

  @Override
  public void onConfirm(ActionEvent actionEvent, Scene scene) {

  }

  @Override
  public List<Node> getRootChildren() {
    return List.of(
      manRadioButton,
      warriorRadioButton,
      guardEdgevilleRadioButton,
      guardVarrockRadioButton,
      new Separator(),
      eatCheckBox
    );
  }

  private RadioButton getRadioButton(String label, boolean defaultValue) {
    RadioButton radioButton = new RadioButton(label);
    radioButton.setSelected(preferences.getBoolean(label, defaultValue));
    radioButton.setStyle("-fx-text-fill: white;");
    radioButton.setToggleGroup(radioButtonGroup);
    return radioButton;
  }

  private CheckBox getCheckBox(String label, boolean defaultValue) {
    CheckBox checkBox = new CheckBox(label);
    checkBox.setSelected(preferences.getBoolean(label, defaultValue));
    checkBox.setStyle("-fx-text-fill: white;");
    return checkBox;
  }
}

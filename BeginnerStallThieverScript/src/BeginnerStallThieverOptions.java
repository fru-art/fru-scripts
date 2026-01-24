import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import java.util.List;

public class BeginnerStallThieverOptions extends Options {
  public final CheckBox bankCheckbox;

  public final RadioButton bakersStallRadioButton;
  public final RadioButton fruitStallRadioButton;
  public final RadioButton teaStallRadioButton;

  public BeginnerStallThieverOptions(ToolkitScript script) {
    super(script);

    bankCheckbox = getCheckBox("Bank", false);

    ToggleGroup toggleGroup = new ToggleGroup();
    bakersStallRadioButton = getRadioButton("Baker's stall (Ardougne market)", true, toggleGroup);
    bakersStallRadioButton.selectedProperty().addListener((obs, prev, next) -> this.updateUi());
    fruitStallRadioButton = getRadioButton("Fruit stall (Hosidius house)", false, toggleGroup);
    fruitStallRadioButton.selectedProperty().addListener((obs, prev, next) -> this.updateUi());
    teaStallRadioButton = getRadioButton("Tea stall (southeast Varrock)", false, toggleGroup);
    teaStallRadioButton.selectedProperty().addListener((obs, prev, next) -> this.updateUi());

    this.updateUi();
  }

  public void updateUi() {
    // TODO: Add hybrid dropping/banking behavior when OSMB supports walking over large item piles
//    bankCheckbox.setText(
//      bakersStallRadioButton.isSelected() ?
//        "Bank (still drops chocolate slices)" :
//        fruitStallRadioButton.isSelected() ?
//          "Bank (still drops apples and bananas)" :
//          "Bank"
//    );
  }

  @Override
  public List<Node> getChildren() {
    return List.of(
      new Text("Start the script somewhere near the selected stall\nor corresponding bank"),
      getHSeparator(),
      bakersStallRadioButton,
      teaStallRadioButton,
      fruitStallRadioButton,
      getHSeparator(),
      bankCheckbox
    );
  }
}

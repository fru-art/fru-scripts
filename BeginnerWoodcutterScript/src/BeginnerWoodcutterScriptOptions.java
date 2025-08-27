import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeginnerWoodcutterScriptOptions extends ScriptOptions {
  private static final String NORMAL_TREE_KEY = "NORMAL_TREE";
  private static final String OAK_TREE_KEY = "OAK_TREE";

  public final CheckBox normalTreeCheckBox;
  public final CheckBox oakTreeCheckBox;

  public Set<Integer> cuttableItems;
  public List<String> cuttableTreeNames;

  public BeginnerWoodcutterScriptOptions(Script script) {
    super(script);

    this.normalTreeCheckBox = getCheckBox("Normal Tree", NORMAL_TREE_KEY, true);
    this.oakTreeCheckBox = getCheckBox("Oak Tree", OAK_TREE_KEY, true);
  }

  @Override
  public void onConfirm(ActionEvent actionEvent, Scene scene) {
    preferences.putBoolean(NORMAL_TREE_KEY, normalTreeCheckBox.isSelected());
    preferences.putBoolean(OAK_TREE_KEY, oakTreeCheckBox.isSelected());

    cuttableItems = Stream.of(
        normalTreeCheckBox.isSelected() ? ItemID.LOGS : null,
        oakTreeCheckBox.isSelected() ? ItemID.OAK_LOGS : null)
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableSet());
    cuttableTreeNames = Stream.of(
        normalTreeCheckBox.isSelected() ? "Tree" : null,
        oakTreeCheckBox.isSelected() ? "Oak tree" : null)
      .filter(Objects::nonNull)
      .toList();

    script.log(
      getClass(),
      "Captured intent for the following trees: " + String.join(", ", cuttableTreeNames));
  }

  @Override
  public List<Node> getRootChildren() {
    return new ArrayList<>(List.of(normalTreeCheckBox, oakTreeCheckBox));
  }

  private CheckBox getCheckBox(String label, String preferenceKey, boolean defaultValue) {
    CheckBox checkBox = new CheckBox(label);
    checkBox.setSelected(preferences.getBoolean(preferenceKey, defaultValue));
    checkBox.setStyle("-fx-text-fill: white;");
    return checkBox;
  }
}

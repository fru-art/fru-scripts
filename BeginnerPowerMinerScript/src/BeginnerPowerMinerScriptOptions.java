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

public class BeginnerPowerMinerScriptOptions extends ScriptOptions {
  private static final String COPPER_ORE_KEY = "COPPER_ORE";
  private static final String TIN_ORE_KEY = "TIN_ORE";
  private static final String IRON_ORE_KEY = "IRON_ORE";

  public final CheckBox copperOreCheckBox;
  public final CheckBox tinOreCheckBox;
  public final CheckBox ironOreCheckBox;

  public List<String> mineableRockNames;
  public Set<Integer> mineables;


  public BeginnerPowerMinerScriptOptions(Script script) {
    super(script);

    this.copperOreCheckBox = getCheckBox("Copper Ore", COPPER_ORE_KEY, false);
    this.tinOreCheckBox = getCheckBox("Tin Ore", TIN_ORE_KEY, false);
    this.ironOreCheckBox = getCheckBox("Iron Ore", IRON_ORE_KEY, true);
  }

  @Override
  public void onConfirm(ActionEvent actionEvent, Scene scene) {
    PREFERENCES.putBoolean(COPPER_ORE_KEY, copperOreCheckBox.isSelected());
    PREFERENCES.putBoolean(TIN_ORE_KEY, tinOreCheckBox.isSelected());
    PREFERENCES.putBoolean(IRON_ORE_KEY, ironOreCheckBox.isSelected());

    mineableRockNames = getMineableRockNames();
    mineables = getMineables();
    script.log(
      getClass(),
      "Captured intent for the following rocks: " + String.join(", ", mineableRockNames));
  }

  @Override
  public List<Node> getRootChildren() {
    return new ArrayList<>(List.of(this.copperOreCheckBox, this.tinOreCheckBox, this.ironOreCheckBox));
  }

  private Set<Integer> getMineables() {
    Set<Integer> mineableOres = Stream.of(
        copperOreCheckBox.isSelected() ? ItemID.COPPER_ORE : null,
        tinOreCheckBox.isSelected() ? ItemID.TIN_ORE : null,
        ironOreCheckBox.isSelected() ? ItemID.IRON_ORE : null)
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableSet());

    return Stream
      .concat(BeginnerPowerMinerScript.GEMS.stream(), mineableOres.stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  private List<String> getMineableRockNames() {
    return Stream.of(
        copperOreCheckBox.isSelected() ? "Copper rocks" : null,
        tinOreCheckBox.isSelected() ? "Tin rocks" : null,
        ironOreCheckBox.isSelected() ? "Iron rocks" : null)
      .filter(Objects::nonNull)
      .toList();
  }

  private CheckBox getCheckBox(String label, String preferenceKey, boolean defaultValue) {
    CheckBox checkBox = new CheckBox(label);
    checkBox.setSelected(PREFERENCES.getBoolean(preferenceKey, defaultValue));
    checkBox.setStyle("-fx-text-fill: white;");
    return checkBox;
  }
}

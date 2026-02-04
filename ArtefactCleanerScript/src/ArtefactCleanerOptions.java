import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;

import java.util.List;
import java.util.Map;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class ArtefactCleanerOptions extends Options {
  public final CheckBox buryBigBonesCheckbox = this.getCheckBox("Bury big bones", false);
  public final CheckBox buryBonesCheckbox = this.getCheckBox("Bury bones", false);
  public final CheckBox equipIronBoltsCheckbox = this.getCheckBox("Equip iron bolts", false);
  public final CheckBox equipIronKnivesCheckbox = this.getCheckBox("Equip iron knives", false);
  public final CheckBox keepCoinsCheckbox = this.getCheckBox("Keep coins (not working currently)", true);
  public final Dropdown skillDropdown;
  public final Map<String, Integer> skillDropdownOptions;

  public ArtefactCleanerOptions(ToolkitScript script) {
    super(script);

    this.keepCoinsCheckbox.setSelected(false);
    this.keepCoinsCheckbox.setDisable(true);
    this.skillDropdownOptions = Map.of(
      "Defence",
      199,
      "Farming",
      217,
      "Herblore",
      205,
      "Hitpoints",
      203,
      "Hunter",
      220,
      "Prayer",
      201,
      "Ranged",
      200,
      "Sailing",
      228,
      "Slayer",
      216,
      "Strength",
      198);
    this.skillDropdown =
      this.getDropdown("Skill: ", this.skillDropdownOptions.keySet().stream().sorted().toList(), "Slayer");
  }

  @Override
  public List<Node> getChildren() {
    return List.of(
      this.skillDropdown.node,
      getHSeparator(),
      this.buryBonesCheckbox,
      this.buryBigBonesCheckbox,
      this.equipIronBoltsCheckbox,
      this.equipIronKnivesCheckbox,
      this.keepCoinsCheckbox);
  }

  public int getSelectedSkillSprite() {
    try {
      return this.skillDropdownOptions.get(this.skillDropdown.comboBox.getValue());
    } catch (NullPointerException e) {
      return -1;
    }
  }
}

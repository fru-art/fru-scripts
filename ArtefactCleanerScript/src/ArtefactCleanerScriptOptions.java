import com.osmb.api.script.Script;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ArtefactCleanerScriptOptions extends ScriptOptions {
  public final CheckBox buryBigBonesCheckbox;
  public final CheckBox buryBonesCheckbox;
  public final CheckBox equipIronBoltsCheckbox;
  public final CheckBox equipIronKnivesCheckbox;
  public final CheckBox keepCoinsCheckbox;
  public final Dropdown skillDropdown;
  public final CheckBox spamGatherRocksCheckbox;
  public final Map<String, Integer> skillDropdownOptions;

  public ArtefactCleanerScriptOptions(Script script) {
    super(script);

    buryBigBonesCheckbox = getCheckBox("Bury big bones", false);
    buryBonesCheckbox = getCheckBox("Bury bones", false);
    equipIronBoltsCheckbox = getCheckBox("Equip iron bolts", false);
    equipIronKnivesCheckbox = getCheckBox("Equip iron knives", false);
    keepCoinsCheckbox = getCheckBox("Keep coins (not working currently)", true);
    keepCoinsCheckbox.setSelected(false);
    keepCoinsCheckbox.setDisable(true);

    skillDropdownOptions = Map.of(
      "Defence", 199,
      "Farming", 217,
      "Herblore", 205,
      "Hitpoints", 203,
      "Ranged", 200,
      "Slayer", 216,
      "Strength", 198
    );
    skillDropdown = getDropdown(
      "Skill: ", "Slayer", skillDropdownOptions.keySet().stream().sorted().toList());

    spamGatherRocksCheckbox = getCheckBox("Spam gather rocks (use at your own risk)", true);
  }

  @Override
  public List<Node> getRootChildren() {
    return List.of(
      skillDropdown.hBox,
      new Separator(),
//      Spacers.vSpacer(0),
      buryBonesCheckbox,
      buryBigBonesCheckbox,
      equipIronBoltsCheckbox,
      equipIronKnivesCheckbox,
      keepCoinsCheckbox,
//      Spacers.vSpacer(0),
      new Separator(),
//      Spacers.vSpacer(0),
      spamGatherRocksCheckbox
//      Spacers.vSpacer(0)
    );
  }

  public int getSelectedSkillSprite() {
    try {
      return skillDropdownOptions.get(skillDropdown.comboBox.getValue());
    } catch (NullPointerException e) {
      return -1;
    }
  }
}

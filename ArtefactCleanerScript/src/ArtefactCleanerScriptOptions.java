import com.osmb.api.script.Script;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;

public class ArtefactCleanerScriptOptions
extends ScriptOptions {
    public final CheckBox buryBigBonesCheckbox = this.getCheckBox("Bury big bones", false);
    public final CheckBox buryBonesCheckbox = this.getCheckBox("Bury bones", false);
    public final CheckBox equipIronBoltsCheckbox = this.getCheckBox("Equip iron bolts", false);
    public final CheckBox equipIronKnivesCheckbox = this.getCheckBox("Equip iron knives", false);
    public final CheckBox keepCoinsCheckbox = this.getCheckBox("Keep coins (not working currently)", true);
    public final Dropdown skillDropdown;
    public final CheckBox spamGatherRocksCheckbox;
    public final Map<String, Integer> skillDropdownOptions;

    public ArtefactCleanerScriptOptions(Script script) {
        super(script);
        this.keepCoinsCheckbox.setSelected(false);
        this.keepCoinsCheckbox.setDisable(true);
        this.skillDropdownOptions = Map.of(
          "Defence", 199,
          "Farming", 217,
          "Herblore", 205,
          "Hitpoints", 203,
          "Hunter", 220,
          "Ranged", 200,
          "Sailing", 228,
          "Slayer", 216,
          "Strength", 198);
        this.skillDropdown = this.getDropdown("Skill: ", "Slayer", this.skillDropdownOptions.keySet().stream().sorted().toList());
        this.spamGatherRocksCheckbox = this.getCheckBox("Spam gather rocks (currently unavailable)", false);
        this.spamGatherRocksCheckbox.setDisable(true);
    }

    @Override
    public List<Node> getRootChildren() {
        return List.of(this.skillDropdown.hBox, new Separator(), this.buryBonesCheckbox, this.buryBigBonesCheckbox, this.equipIronBoltsCheckbox, this.equipIronKnivesCheckbox, this.keepCoinsCheckbox, new Separator(), this.spamGatherRocksCheckbox);
    }

    public int getSelectedSkillSprite() {
        try {
            return this.skillDropdownOptions.get(this.skillDropdown.comboBox.getValue());
        }
        catch (NullPointerException e) {
            return -1;
        }
    }
}

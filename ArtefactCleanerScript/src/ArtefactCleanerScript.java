import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
@ScriptDefinition(author = "fru",
  name = "Artefact Cleaner",
  threadUrl = "https://wiki.osmb.co.uk/article/artefact-cleaner",
  skillCategory = SkillCategory.OTHER,
  version = 2.02)
public class ArtefactCleanerScript extends ToolkitScript {
  public static final Set<Integer> ARTEFACTS = Set.of(11176, 11177, 11183, 11178);
  public static final Set<Integer> DROPS = new HashSet<Integer>(Set.of(
    11180,
    11181,
    532,
    526,
    1923,
    687,
    1469,
    9420,
    11195,
    453,
    617,
    995,
    6964,
    8890,
    436,
    40,
    9140,
    1203,
    807,
    863,
    440,
    447,
    11179,
    11182,
    1931,
    438,
    1627,
    1625,
    9440));
  public static final Set<Integer> INTERACTABLES = new HashSet<Integer>(Set.of());
  public static final Set<Integer> TOOLS = Set.of(675, 670, 676);
  public static final Set<Integer> allItems = Stream.of(Set.of(4447, 11175), ARTEFACTS, DROPS, INTERACTABLES, TOOLS)
    .flatMap(Collection::stream)
    .collect(Collectors.toSet());
  public final ArtefactCleanerOptions scriptOptions = new ArtefactCleanerOptions(this);

  public ArtefactCleanerScript(Object scriptCore) {
    super(scriptCore);
  }

  public void onStart() {
    super.onStart();
    this.scriptOptions.show();
    if (this.scriptOptions.getSelectedSkillSprite() == -1) {
      Object skill = this.scriptOptions.skillDropdown.comboBox.getValue();
      this.log(
        ((Object) ((Object) this)).getClass(),
        "Failed to retrieve sprite for selected skill: " + String.valueOf(skill));
      this.stop();
      return;
    }
    if (this.scriptOptions.buryBigBonesCheckbox.isSelected()) {
      DROPS.remove(532);
      INTERACTABLES.add(532);
    }
    if (this.scriptOptions.buryBonesCheckbox.isSelected()) {
      DROPS.remove(526);
      INTERACTABLES.add(526);
    }
    if (this.scriptOptions.equipIronBoltsCheckbox.isSelected()) {
      DROPS.remove(9140);
      INTERACTABLES.add(9140);
    }
    if (this.scriptOptions.equipIronKnivesCheckbox.isSelected()) {
      DROPS.remove(863);
      INTERACTABLES.add(863);
    }
  }

  @Override
  protected List<Job> getJobs() {
    return List.of(
      new OpenLampsJob(this),
      new CleanFindsJob(this),
      new TurnInArtefactsJob(this),
      new CleanInventoryJob(this),
      new TakeFindsJob(this));
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(12853, 13109);
  }

  @Override
  public String getAuthorLogo() {
    return "https://cdn.discordapp.com/avatars/236104938592272390/a_e796f8b2a87385d3360bef7dc6916bb0.png?size=128";
  }

  @Override
  public String getLogo() {
    return "https://oldschool.runescape.wiki/images/thumb/Uncleaned_find_detail.png/514px-Uncleaned_find_detail" +
      ".png?1e5bc";
  }

  @Override
  protected String getVersionSourceUrl() {
    return "https://github.com/fru-art/fru-scripts/blob/master/ArtefactCleanerScript/src/ArtefactCleanerScript.java";
  }
}

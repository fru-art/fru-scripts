import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.job.impl.DepositAtBankJob;
import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ScriptDefinition(
  name = "Flax Picker", author = "John Flaxson", version = 1.0, threadUrl = "", skillCategory = SkillCategory.CRAFTING)
public class FlaxPickerScript extends ToolkitScript {
  public static final int NEMUS_RETREAT = 5427;
  public static final Set<Integer> SEERS_VILLAGE = Set.of(10805, 10806);

  public final FlaxPickerOptions options;

  public FlaxPickerScript(Object scriptCore) {
    super(scriptCore);
    options = new FlaxPickerOptions(this);
  }

  @Override
  public List<Job> getJobs() {
      return List.of(
        new PickFlaxJob(this),
        new DepositAtBankJob(this)
      );
  }

  @Override
  public List<Integer> getRequiredRegions() {
    return Stream.concat(
      Stream.of(NEMUS_RETREAT),
      SEERS_VILLAGE.stream()
    ).toList();
  }

  @Override
  public String getAuthorLogo() {
    // Tip: You can use browser Discord to 'Inspect element' and grab the freely-hosted profile picture from your
    // Discord profile (don't forget to replace .webp with .png)
    return "https://cdn.discordapp.com/avatars/1466566655709806720/14dd9a1ef2ee2cde375b8f05c49ef8db.png?size=128";
  }

  @Override
  public String getLogo() {
    // Tip: You can click on any image in the OSRS Wiki and use those image URLs to reference here
    return "https://oldschool.runescape.wiki/images/thumb/Flax_detail.png/591px-Flax_detail.png?c7fb9";
  }

  @Override
  public List<Options> getOptions() {
    return List.of(options);
  }

  @Override
  public String getVersionSource() {
    // Tip: If your source is private, you can instead expose a version.txt file to read from
    return "https://raw.githubusercontent.com/fru-art/fru-scripts/refs/heads/master/deprecated/FlaxPickerScript/src/FlaxPickerScript.java";
  }
}

import com.osmb.api.item.ItemID;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.job.impl.BankDepositJob;
import com.osmbtoolkit.job.impl.DropJob;
import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@ScriptDefinition(author = "fru", name = "Stall Thiever", description = "for stealing from basic stalls",
  skillCategory = SkillCategory.THIEVING, version = 1.0)
public class StallThieverScript extends ToolkitScript {
  public static final Set<Integer> BAKED_GOODS =
    Set.of(ItemID.BREAD, ItemID.CAKE, ItemID.CHOCOLATE_SLICE, ItemID.SLICE_OF_CAKE);
  public static final Set<Integer> FRUITS = Set.of(
    ItemID.COOKING_APPLE,
    ItemID.BANANA,
    ItemID.STRAWBERRY,
    ItemID.JANGERBERRIES,
    ItemID.LEMON,
    ItemID.REDBERRIES,
    ItemID.PINEAPPLE,
    ItemID.LIME,
    ItemID.STRANGE_FRUIT,
    ItemID.GOLOVANOVA_FRUIT_TOP,
    ItemID.PAPAYA_FRUIT);
  public static final Set<Integer> TEA = Set.of(ItemID.CUP_OF_TEA, ItemID.EMPTY_CUP);

  public final Hosidius hosidius;
  public final StallThieverOptions scriptOptions;

  public StallThieverScript(Object scriptCore) {
    super(scriptCore);
    this.hosidius = new Hosidius(this);
    this.scriptOptions = new StallThieverOptions(this);
  }

  @Override
  protected List<Job> getJobs() {
    if (scriptOptions.bakersStallRadioButton.isSelected()) {
      return List.of(
        scriptOptions.bankCheckbox.isSelected() ? new BankDepositJob(this) : new DropJob(this, BAKED_GOODS),
        new StealFromBakeryStallJob(this));
    }

    if (scriptOptions.fruitStallRadioButton.isSelected()) {
      return List.of(
        scriptOptions.bankCheckbox.isSelected() ? new HosidiusBankDepositJob(this) : new DropJob(this, FRUITS),
        new StealFromFruitStallJob(this));
    }

    if (scriptOptions.teaStallRadioButton.isSelected()) {
      return List.of(
        scriptOptions.bankCheckbox.isSelected() ? new BankDepositJob(this) : new DropJob(this, TEA),
        new StealFromTeaStallJob(this));
    }

    return Collections.emptyList();
  }

  @Override
  public String getAuthorLogo() {
    return "https://cdn.discordapp.com/avatars/236104938592272390/a_e796f8b2a87385d3360bef7dc6916bb0.png?size=128";
  }

  @Override
  public String getLogo() {
    return "https://oldschool.runescape.wiki/images/thumb/Silk_stall.png/474px-Silk_stall.png?def4e";
  }

  @Override
  protected List<Options> getOptions() {
    return List.of(this.scriptOptions);
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(
      10547, // Baker's stall
      12853, // Varrock bank
      13109, // Tea stall
      6968, 7223, 7224 // Hosidius
    );
  }

  @Override
  protected String getSourceUrl() {
    return "https://github.com/fru-art/fru-scripts/blob/master/StallThieverScript/src/StallThieverScript.java";
  }
}

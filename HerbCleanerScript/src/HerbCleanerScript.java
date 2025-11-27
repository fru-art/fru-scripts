import com.osmb.api.item.ItemID;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.tabs.Skill;
import task.BankDepositTask;
import task.BankWithdrawTask;
import task.SequenceTaskScript;
import task.Task;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Herb Cleaner",
  description = "for cleaning herbs for XP and profit",
  skillCategory = SkillCategory.HERBLORE,
  version = 1.5
)
public class HerbCleanerScript extends SequenceTaskScript {
  public static final Set<Integer> CLEAN_HERBS = Set.of(
    ItemID.GUAM_LEAF,
    ItemID.MARRENTILL,
    ItemID.TARROMIN,
    ItemID.HARRALANDER,
    ItemID.RANARR_WEED,
    ItemID.TOADFLAX,
    ItemID.IRIT_LEAF,
    ItemID.AVANTOE,
    ItemID.KWUARM,
    ItemID.HUASCA,
    ItemID.SNAPDRAGON,
    ItemID.CADANTINE,
    ItemID.LANTADYME,
    ItemID.DWARF_WEED,
    ItemID.TORSTOL
  );
  public static final Set<Integer> GRIMY_HERBS = Set.of(
    ItemID.GRIMY_GUAM_LEAF,
    ItemID.GRIMY_MARRENTILL,
    ItemID.GRIMY_TARROMIN,
    ItemID.GRIMY_HARRALANDER,
    ItemID.GRIMY_RANARR_WEED,
    ItemID.GRIMY_TOADFLAX,
    ItemID.GRIMY_IRIT_LEAF,
    ItemID.GRIMY_AVANTOE,
    ItemID.GRIMY_KWUARM,
    ItemID.GRIMY_HUASCA,
    ItemID.GRIMY_SNAPDRAGON,
    ItemID.GRIMY_CADANTINE,
    ItemID.GRIMY_LANTADYME,
    ItemID.GRIMY_DWARF_WEED,
    ItemID.GRIMY_TORSTOL
  );


  public HerbCleanerScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of(
      new BankDepositTask(this, null, Set.of()),
      new BankWithdrawTask(this, null, GRIMY_HERBS, 0, 28)
        .setIsCritical(true)
        .setRetryLimit(1), // Withdraw occasionally fails for some reason
      new CleanHerbsTask(this)
    );
  }

  @Override
  protected List<Integer> getRegionsToPrioritize() {
    return List.of(
      12597, // West Varrock
      12598 // Grand Exchange
    );
  }

  @Override
  public boolean promptBankTabDialogue() {
    return true;
  }
}

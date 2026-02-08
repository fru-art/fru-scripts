import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import helper.NpcType;
import task.*;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Dark Wizards",
  threadUrl = "https://wiki.osmb.co.uk/article/dark-wizard-killer-legacy",
  skillCategory = SkillCategory.COMBAT,
  version = 1.5
)
public class DarkWizardScript extends SequenceTaskScript {
  private static final Set<Integer> PARTIAL_VARROCK = Set.of(12597, 12853, 12852);

  public DarkWizardScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    Set<BankLocation> bankLocations = Set.of(
      new BankLocation(
        PARTIAL_VARROCK,
        new WorldPosition(3185, 3436, 0)));

    return List.of(
      new BankWithdrawTask(this, bankLocations, KillTask.food).setIsCritical(true),
      new WalkTask(this, new WorldPosition(3226, 3369, 0), 12),
      new DropTask(this, Set.of(ItemID.BONES)), // Sometimes accidentally pick up bones
      new KillTask(this,
        new NpcType("Dark wizard", 0, 190, 0.75, 25),
        Set.of(
          ItemID.AIR_RUNE,
          ItemID.BLOOD_RUNE,
          ItemID.BODY_RUNE,
          ItemID.CHAOS_RUNE,
          ItemID.COSMIC_RUNE,
          ItemID.EARTH_RUNE,
          ItemID.FIRE_RUNE,
          ItemID.LAW_RUNE,
          ItemID.MIND_RUNE,
          ItemID.NATURE_RUNE,
          ItemID.WATER_RUNE))
    );
  }

  @Override
  protected List<Integer> getRegionsToPrioritize() {
    return PARTIAL_VARROCK.stream().toList();
  }
}

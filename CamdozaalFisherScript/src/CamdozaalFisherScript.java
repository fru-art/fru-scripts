import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import task.*;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Camdozaal Fisher",
  threadUrl = "",
  skillCategory = SkillCategory.PRAYER,
  version = 1.9
)

public class CamdozaalFisherScript extends FirstMatchTaskScript {
  private static final Set<Integer> BANKABLES = Set.of(
    ItemID.BARRONITE_HANDLE);
  private static final Set<Integer> DROPPABLES = Set.of(
    ItemID.FROG_SPAWN,
    ItemID.RUINED_CAVEFISH,
    ItemID.RUINED_GUPPY,
    ItemID.RUINED_TETRA,
    ItemID.RUINED_CATFISH);
  private static final Set<Integer> REGIONS = Set.of(11610, 11866);

  public CamdozaalFisherScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    Set<BankLocation> bankLocations = Set.of(new BankLocation(REGIONS, new WorldPosition(2978, 5798, 0)));

    return List.of(
      new PrepareTask(this),
      new OfferTask(this),
      new DropTask(this, DROPPABLES),
      new BankDepositTask(this, bankLocations, BANKABLES, 5),
      new WalkTask(this, new WorldPosition(2931, 5773, 0), 10),
      new FishInCamdozaalTask(this)
    );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return REGIONS.stream().toList();
  }
}

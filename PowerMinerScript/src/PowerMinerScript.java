import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Power Miner",
  description = "Mines and drops up to iron",
  skillCategory = SkillCategory.MINING,
  version = 1.2
)

public class PowerMinerScript extends FirstMatchTaskScript {
  public static final Set<Integer> GEMS = Set.of(
    ItemID.UNCUT_SAPPHIRE,
    ItemID.UNCUT_RUBY,
    ItemID.UNCUT_EMERALD,
    ItemID.UNCUT_DIAMOND
  );
  public static final Set<Integer> VARROCK_EAST_REGIONS = Set.of(12853, 13109, 13108);

  // Needed to convert between position types
  public final ScriptCore scriptCore;

  private final PowerMinerScriptOptions scriptOptions;

  public PowerMinerScript(Object scriptCore){
    super(scriptCore);

    this.scriptCore = (ScriptCore) scriptCore;
    scriptOptions = new PowerMinerScriptOptions(this);
  }

  @Override
  public void onStart() {
    scriptOptions.show();
  }

  @Override
  protected List<Task> getTaskList() {
    Set<BankLocation> bankLocations = Set.of(
      new BankLocation(
        VARROCK_EAST_REGIONS,
        new WorldPosition(3254, 3421, 0)));

    assert scriptOptions != null;
    return List.of(
      // TODO: Remove
//      new BankDepositTask(this, bankLocations, scriptOptions.mineables),
//      new WalkTask(this, new WorldPosition(3282, 3366, 0), 1),
      new DropTask(this, scriptOptions.mineables),
      new MineTask(this, scriptOptions)
    );
  }

  @Override
  protected List<Integer> getRegionsToPrioritize() {
    return List.of(
      8747, // Isle of Souls
      10804, 10803, // Legends' Guild
      11826, // Rimmington
      12183, 12184, 12185, // Mining Guild
      13107, // Al Kharid
      13108 // Southeast Varrock
    );
  }
}

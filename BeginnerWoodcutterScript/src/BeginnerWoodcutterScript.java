import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import task.*;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Beginner Woodcutter",
  description = "for chopping and banking up to oak logs in Varrock",
  skillCategory = SkillCategory.WOODCUTTING,
  version = 1.1
)
public class BeginnerWoodcutterScript extends SequenceTaskScript {
  private static final BankLocation VARROCK_WEST_BANK = new BankLocation(
    Set.of(12597, 12853, 12852),
    new WorldPosition(3183, 3435, 0));

  private final BeginnerWoodcutterScriptOptions scriptOptions;

  public BeginnerWoodcutterScript(Object scriptCore) {
    super(scriptCore);
    scriptOptions = new BeginnerWoodcutterScriptOptions(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    scriptOptions.show();
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of(
      new BankDepositTask(this, Set.of(VARROCK_WEST_BANK), scriptOptions.cuttableItems)
        .setCanLoop(true),
      new WalkTask(this, new WorldPosition(3170, 3415, 0), 15),
      new WoodcutInVarrockTask(this, scriptOptions)
    );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(
      12597, 12598, 12853 // Varrock West
    );
  }
}

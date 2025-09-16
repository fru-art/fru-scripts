import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import helper.NpcType;
import task.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@ScriptDefinition(
  author = "fru",
  name = "Beginner Pickpocketer",
  description = "for low-level thieving XP in Edgeville and Al Kharid",
  skillCategory = SkillCategory.THIEVING,
  version = 0.13
)
public class BeginnerPickpocketerScript extends FirstMatchTaskScript {
  private static final Set<BankLocation> BANKS = Set.of(
    new BankLocation(Set.of(12597, 12854, 12598), new WorldPosition(3185, 3436, 0)), // West Varrock Bank
    new BankLocation(Set.of(12853, 12852), new WorldPosition(3253, 3420, 0)), // East Varrock Bank
    new BankLocation(Set.of(12342, 12343), new WorldPosition(3096,3494, 0)) // Edgeville
  );
  private static final NpcType GUARD =
    new NpcType("Guard", 180, 0.6, 3);
  private static final NpcType MAN =
    new NpcType("Man", 180, 0.6, 2);
  private static final NpcType WARRIOR =
    new NpcType("Warrior", 180, 0.6, 3);
//  private static final NpcType WOMAN =
//    new NpcType("Woman", 180, 0.6, 2);

  private final BeginnerPickpocketerScriptOptions scriptOptions;

  public BeginnerPickpocketerScript(Object scriptCore) {
    super(scriptCore);
    scriptOptions = new BeginnerPickpocketerScriptOptions(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    scriptOptions.show();
  }

  @Override
  protected List<Task> getTaskList() {
    return Stream.of(
      new FailsafeTask(this, Set.of(Failsafe.CANT_REACH)),

      scriptOptions.eatCheckBox.isSelected() ?
        new BankWithdrawTask(this, BANKS, EatTask.food, 0, 24) :
        null,
      scriptOptions.eatCheckBox.isSelected() ?
        new EatTask(this, 3) : // TODO: 3 hard-code needs updating if adding new targets
        null,

      scriptOptions.guardEdgevilleRadioButton.isSelected() ?
        new PickpocketTask(this, GUARD, new WorldPosition(3103, 3510, 0),
          List.of(
            new RectangleArea(3091, 3507, 9, 6, 0),
            new RectangleArea(3103, 3519, 22, 2, 0))) :
        null,
      scriptOptions.guardVarrockCastleRadioButton.isSelected() ?
        new PickpocketTask(this, GUARD, new WorldPosition(3212, 3463, 0)) :
        null,
      scriptOptions.guardVarrockWestBankRadioButton.isSelected() ?
        new PickpocketTask(this, GUARD, new WorldPosition(3172, 3427, 0)) :
        null,
      scriptOptions.manRadioButton.isSelected() ?
        new PickpocketTask(this, MAN, new WorldPosition(3103, 3510, 0),
          List.of(new RectangleArea(3091, 3507, 9, 6, 0))) :
        null,
//      scriptOptions.warriorRadioButton.isSelected() ?
//        new PickpocketTask(this, WARRIOR, ...) :
//        null,

      new OpenAllCoinPouchTask(this))
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(
      13105, // Al-Kharid
      12342, 12343, // Edgeville
      12853, 12854, 13109, // East Varrock
      12597, 12598 // West Varrock
    );
  }
}

import com.osmb.api.item.ItemID;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import task.DropTask;
import task.FirstMatchTaskScript;
import task.Task;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Beginner Stall Thiever",
  description = "for stealing from basic stalls and dropping items",
  skillCategory = SkillCategory.THIEVING,
  version = 1.0
)
public class BeginnerStallThieverScript extends FirstMatchTaskScript {
  public static final Set<Integer> BAKED_GOODS = Set.of(
    ItemID.BREAD,
    ItemID.CAKE,
    ItemID.CHOCOLATE_SLICE
  );
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
    ItemID.PAPAYA_FRUIT
  );

  public final BeginnerStallThieverScriptOptions scriptOptions;

  public BeginnerStallThieverScript(Object scriptCore) {
    super(scriptCore);

    this.scriptOptions = new BeginnerStallThieverScriptOptions(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    scriptOptions.show();
  }

  @Override
  protected List<Task> getTaskList() {
    if (scriptOptions.bakersStallRadioButton.isSelected()) {
      return List.of(
        new DropTask(this, BAKED_GOODS, 20, 28),
        new StealFromBakerTask(this)
      );
    }

    if (scriptOptions.fruitStallRadioButton.isSelected()) {
      return List.of(
        new DropTask(this, FRUITS, 20, 28),
        new StealFruitsTask(this)
      );
    }

    return Collections.emptyList();
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(
      10547, // Baker's stall
      7224 // Fruit stall
    );
  }
}

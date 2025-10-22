import com.osmb.api.item.ItemID;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import task.FirstMatchTaskScript;
import task.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ScriptDefinition(
  author = "fru",
  name = "Artefact Cleaner",
  description = "for slow XP lamps in the Varrock Museum",
  skillCategory = SkillCategory.OTHER,
  version = 1.0
)
public class ArtefactCleanerScript extends FirstMatchTaskScript {
  public static final Set<Integer> ARTEFACTS = Set.of(
    ItemID.ARROWHEADS,
    ItemID.JEWELLERY,
    ItemID.OLD_CHIPPED_VASE,
    ItemID.POTTERY
  );
  public static final Set<Integer> DROPS = new HashSet<>(Set.of(
    ItemID.ANCIENT_COIN,
    ItemID.ANCIENT_SYMBOL,
    ItemID.BIG_BONES,
    ItemID.BONES,
    ItemID.BOWL,
    ItemID.BROKEN_ARROW,
    ItemID.BROKEN_GLASS_1469,
    ItemID.BRONZE_LIMBS,
    ItemID.CLEAN_NECKLACE,
    ItemID.COAL,
    ItemID.COINS, ItemID.COINS_995, ItemID.COINS_6964, ItemID.COINS_8890,
    ItemID.COPPER_ORE,
    ItemID.IRON_ARROWTIPS,
    ItemID.IRON_BOLTS,
    ItemID.IRON_DAGGER,
    ItemID.IRON_DART,
    ItemID.IRON_KNIFE,
    ItemID.IRON_ORE,
    ItemID.MITHRIL_ORE,
    ItemID.OLD_COIN,
    ItemID.OLD_SYMBOL,
    ItemID.POT,
    ItemID.TIN_ORE,
    ItemID.UNCUT_JADE,
    ItemID.UNCUT_OPAL,
    ItemID.WOODEN_STOCK
  ));
  public static final Set<Integer> INTERACTABLES = new HashSet<>(Set.of(
//    ItemID.BIG_BONES,
//    ItemID.BONES,
//    ItemID.IRON_BOLTS,
//    ItemID.IRON_KNIFE
  ));
  public static final Set<Integer> TOOLS = Set.of(
    ItemID.ROCK_PICK,
    ItemID.SPECIMEN_BRUSH,
    ItemID.TROWEL
  );

  public static final Set<Integer> allItems = Stream.of(
    Set.of(ItemID.ANTIQUE_LAMP, ItemID.UNCLEANED_FIND),
    ARTEFACTS,
    DROPS,
    INTERACTABLES,
    TOOLS)
    .flatMap(Set::stream)
    .collect(Collectors.toSet());

  public final ArtefactCleanerScriptOptions scriptOptions;

  public ArtefactCleanerScript(Object scriptCore) {
    super(scriptCore);
    scriptOptions = new ArtefactCleanerScriptOptions(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    scriptOptions.show();

    if (scriptOptions.getSelectedSkillSprite() == -1) {
      Object skill = scriptOptions.skillDropdown.comboBox.getValue();
      log(getClass(), "Failed to retrieve sprite for selected skill: " + skill);
      stop();
      return;
    }

    if (scriptOptions.buryBigBonesCheckbox.isSelected()) {
      DROPS.remove(ItemID.BIG_BONES);
      INTERACTABLES.add(ItemID.BIG_BONES);
    }
    if (scriptOptions.buryBonesCheckbox.isSelected()) {
      DROPS.remove(ItemID.BONES);
      INTERACTABLES.add(ItemID.BONES);
    }
    if (scriptOptions.equipIronBoltsCheckbox.isSelected()) {
      DROPS.remove(ItemID.IRON_BOLTS);
      INTERACTABLES.add(ItemID.IRON_BOLTS);
    }
    if (scriptOptions.equipIronKnivesCheckbox.isSelected()) {
      DROPS.remove(ItemID.IRON_KNIFE);
      INTERACTABLES.add(ItemID.IRON_KNIFE);
    }
//    if (!scriptOptions.keepCoinsCheckbox.isSelected()) {
//      DROPS.remove(ItemID.COINS);
//      // Make sure coins are still in the all item set
//    }
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of(
      new OpenLampsTask(this),
      new CleanFindsTask(this),
      new TurnInArtefacts(this),
      new CleanInventoryTask(this),
      new TakeFindsTask(this)
    );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(12853, 13109);
  }
}

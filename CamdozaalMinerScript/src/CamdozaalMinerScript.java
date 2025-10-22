import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import helper.InventoryHelper;
import task.*;

import java.util.List;
import java.util.Set;

@ScriptDefinition(
  author = "fru",
  name = "Camdozaal Miner",
  description = "Mines barronite deposits and opens them and banks loots",
  skillCategory = SkillCategory.MINING,
  version = 1.5
)

public class CamdozaalMinerScript extends FirstMatchTaskScript {
  private static final Set<Integer> BANKABLES = Set.of(
    ItemID.ANCIENT_ASTROSCOPE,
    ItemID.ANCIENT_CARCANET,
    ItemID.ANCIENT_GLOBE,
    ItemID.ANCIENT_LEDGER,
    ItemID.ANCIENT_TREATISE,
    ItemID.BARRONITE_HEAD,
    ItemID.IMCANDO_HAMMER_BROKEN);
  private static final Set<Integer> GEMS = Set.of(
    ItemID.UNCUT_DIAMOND,
    ItemID.UNCUT_EMERALD,
    ItemID.UNCUT_RUBY,
    ItemID.UNCUT_SAPPHIRE);
  private static final Set<Integer> MINEABLES = Set.of(
    ItemID.BARRONITE_SHARDS,
    ItemID.BARRONITE_DEPOSIT,
    ItemID.UNCUT_DIAMOND,
    ItemID.UNCUT_EMERALD,
    ItemID.UNCUT_RUBY,
    ItemID.UNCUT_SAPPHIRE);
  private static final Set<Integer> REGIONS = Set.of(11610, 11866);

  private final ScriptCore scriptCore;

  private final InventoryHelper inventoryHelper;

  public CamdozaalMinerScript(Object scriptCore) {
    super(scriptCore);
    this.scriptCore = (ScriptCore) scriptCore;
    this.inventoryHelper = new InventoryHelper(this, MINEABLES);
  }

  @Override
  protected List<Task> getTaskList() {
    Set<BankLocation> bankLocations = Set.of(new BankLocation(REGIONS, new WorldPosition(2978, 5798, 0)));
    ItemGroupResult inventorySnapshot = this.inventoryHelper.getSnapshot();

    return List.of(
      new ProcessAtObjectTask(
        this,
        "Barronite crusher",
        "Smith",
        new WorldPosition(2957, 5807, 0),
        List.of(ItemID.BARRONITE_DEPOSIT),
        false),
      new BankDepositTask(this, bankLocations, BANKABLES, 0),
      // Inventory will never be filled with gems, so this will only execute when already at the bank due to previous
      // task i.e. not interrupt mining.
      new BankDepositTask(this, bankLocations, GEMS, 28),
      new WalkTask(this, new WorldPosition(2937, 5807, 0), 8),
      new MineTask(
        this,
        this.scriptCore,
        List.of("Barronite rocks"),
        MINEABLES,
        true,
        260,
        7).setCanLoop(true)
      );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return REGIONS.stream().toList();
  }
}

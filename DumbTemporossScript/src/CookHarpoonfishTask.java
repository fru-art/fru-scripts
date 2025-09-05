import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import helper.InventoryHelper;
import helper.ObjectHelper;
import task.Task;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CookHarpoonfishTask extends Task {
  private static final Map<Island, WorldPosition> NEAR_SHRINE = Map.of(
    Island.NORTH, new WorldPosition(3041, 3001, 0),
    Island.SOUTH, new WorldPosition(3037, 2956, 0)
  );
  private final TemporossScript script;

  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;

  public CookHarpoonfishTask(TemporossScript script) {
    super(script);
    this.script = script;

    inventoryHelper = new InventoryHelper(script,
      Set.of(ItemID.HARPOONFISH, ItemID.RAW_HARPOONFISH));
    objectHelper = new ObjectHelper(script);
  }

  @Override
  public boolean canExecute() {
    if (script.canExecuteInterruptTask()) return false;

    Island island = script.getIsland();
    if (island == null) return false;

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.contains(ItemID.RAW_HARPOONFISH);
  }

  @Override
  public boolean execute() {
    Island island = script.getIsland();
    assert island != null;

    AtomicInteger cookedCount = new AtomicInteger(
      inventoryHelper.getSnapshot().getAmount(ItemID.HARPOONFISH));
    WorldPosition nearShrine = NEAR_SHRINE.get(island);
    boolean result = objectHelper.walkToAndTap(
      "Shrine", nearShrine, false, "Cook-at", () -> !canExecute());
    if (!result) {
      script.log(getClass(), "Failed to tap on shrine");
    }
    if (!canExecute()) return true;

    AtomicBoolean exit = new AtomicBoolean(false);
    while (canExecute() && !exit.get()) {
      result = script.submitHumanTask(
        () -> {
          if (!canExecute()) return true;

          int nextCookedCount = inventoryHelper.getSnapshot().getAmount(ItemID.HARPOONFISH);
          if (nextCookedCount == cookedCount.get()) return false;
          if (nextCookedCount < cookedCount.get()) {
            script.log(getClass(), "Lost fish for an unknown reason");
            exit.set(true);
            return true;
          }

          cookedCount.set(nextCookedCount);
          return true;
        },
        1_800); // 3 ticks
      if (!result) return true;
      if (inventoryHelper.getSnapshot().getAmount(ItemID.RAW_HARPOONFISH) == 0) return true;
    }

    return true;
  }
}

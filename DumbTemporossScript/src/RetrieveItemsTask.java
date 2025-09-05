import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import helper.InventoryHelper;
import helper.ObjectHelper;
import task.Task;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RetrieveItemsTask extends Task {
  private static final Map<Island, WorldPosition> NEAR_AMMUNITION = Map.of(
    Island.NORTH, new WorldPosition(3037, 2979, 0),
    Island.SOUTH, new WorldPosition(3057, 2976, 9)
  );
  private static final Map<Island, WorldPosition> NEAR_TOOLS = Map.of(
    Island.NORTH, new WorldPosition(3034, 2975, 0),
    Island.SOUTH, new WorldPosition(3060, 2979, 0)
  );
  private static final Set<Integer> TOOLS = Set.of(
    ItemID.HARPOON,
    ItemID.HAMMER,
    ItemID.ROPE
  );

  private final TemporossScript script;

  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;

  public RetrieveItemsTask(TemporossScript script) {
    super(script);
    isCritical = true;
    retryLimit = 3;

    this.script = script;

    inventoryHelper = new InventoryHelper(script, Stream.concat(
      TOOLS.stream(),
      Stream.of(ItemID.RAW_HARPOONFISH)
    ).collect(Collectors.toSet()));
    objectHelper = new ObjectHelper(script);
  }

  @Override
  public boolean canExecute() {
    Island island = script.getIsland();
    if (island == null) return false;

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    if (inventoryHelper.getSnapshot().containsAll(TOOLS)) return false;

    if (script.isNearBoat()) return true;

    // If not near boat, script should finish cooking raw harpoonfish if applicable
    if (snapshot.containsAny(ItemID.RAW_HARPOONFISH)) return false;

    return true;
  }

  @Override
  public boolean execute() {
    Island island = script.getIsland();
    assert island != null;
    WorldPosition nearTools = NEAR_TOOLS.get(island);
    assert nearTools != null;

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();

    if (!snapshot.contains(ItemID.ROPE)) {
      objectHelper.walkToAndTap("Ropes", nearTools);
      if (!script.submitTask(() -> inventoryHelper.getSnapshot().contains(ItemID.ROPE), 600)) {
        script.log(getClass(), "Failed to retrieve rope");
        return false;
      }
    }

    if (script.canExecuteInterruptTask()) return true;
    if (!snapshot.contains(ItemID.HAMMER)) {
      objectHelper.walkToAndTap("Hammers", nearTools);
      if (!script.submitTask(() -> inventoryHelper.getSnapshot().contains(ItemID.HAMMER), 600)) {
        script.log(getClass(), "Failed to retrieve hammer");
        return false;
      }
    }

    if (script.canExecuteInterruptTask()) return true;
    if (!snapshot.contains(ItemID.HARPOON)) {
      objectHelper.walkToAndTap("Harpoons", nearTools);
      if (!script.submitTask(() -> inventoryHelper.getSnapshot().contains(ItemID.HARPOON), 600)) {
        script.log(getClass(), "Failed to retrieve harpoon");
        return false;
      }
    }

    return true;
  }
}

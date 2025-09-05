import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.walker.WalkConfig;
import helper.DrawHelper;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LoadHarpoonfishTask extends Task {
  private static final Map<Island, WorldPosition> AMMUNITION = Map.of(
    Island.NORTH, new WorldPosition(3038, 2978, 0),
    Island.SOUTH, new WorldPosition(3056, 2976, 0)
  );
  private static final Map<Island, WorldPosition> NEAR_AMMUNITION = Map.of(
    Island.NORTH, new WorldPosition(3037, 2978, 0),
    Island.SOUTH, new WorldPosition(3057, 2976, 0)
  );

  private final DumbTemporossScript script;

  private final DrawHelper drawHelper;
  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;


  public LoadHarpoonfishTask(DumbTemporossScript script) {
    super(script);
    this.script = script;

    drawHelper = new DrawHelper(script);
    inventoryHelper = new InventoryHelper(script,
      Set.of(ItemID.HARPOONFISH, ItemID.RAW_HARPOONFISH));
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    if (script.canExecuteInterruptTask()) return false;

    Island island = script.getIsland();
    if (island == null) return false;

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.contains(ItemID.HARPOONFISH);
  }

  @Override
  public boolean execute() {
    Island island = script.getIsland();
    assert island != null;

    AtomicInteger cookedCount = new AtomicInteger(
      inventoryHelper.getSnapshot().getAmount(ItemID.HARPOONFISH));

    boolean result = walkToAndTapAmmunition(island);
    if (!result) {
      script.log(getClass(), "Failed to tap on ammunition crate");
      return false;
    }

    while (canExecute()) {
      result = script.submitHumanTask(
        () -> {
          if (!canExecute()) return true;
          getAmmunitionCube(island); // Draw

          int nextCookedCount = inventoryHelper.getSnapshot().getAmount(ItemID.HARPOONFISH);
          if (nextCookedCount == cookedCount.get()) return false;

          cookedCount.set(nextCookedCount);
          return true;
        },
        1_800); // 3 ticks

      if (!result) return true;
      if (inventoryHelper.getSnapshot().getAmount(ItemID.HARPOONFISH) == 0) return true;
    }

    return true;
  }

  // TODO: Migrate to Object API
  private boolean walkToAndTapAmmunition(Island island) {
    AtomicReference<Polygon> ammunitionCube = new AtomicReference<>(getAmmunitionCube(island));

    if (ammunitionCube.get() == null) {
      WorldPosition nearAmmunition = NEAR_AMMUNITION.get(island);
      if (nearAmmunition == null) return false;

      WalkConfig walkConfig = new WalkConfig.Builder()
        .breakCondition(() -> {
          if (!canExecute()) return true;
          return getAmmunitionCube(island) != null;
        })
        .breakDistance(5)
        .build();
      script.getWalker().walkTo(nearAmmunition, walkConfig);
      if (!canExecute()) return true;
    }

    ammunitionCube.set(getAmmunitionCube(island));
    if (ammunitionCube.get() == null) {
      script.log(getClass(), "Failed to walk to ammunition crate");
      return false;
    }

    script.getFinger().tapGameScreen(getAmmunitionCube(island));

    AtomicBoolean longTapped = new AtomicBoolean(false);
    boolean result = waitHelper.waitForNoChange("position", script::getWorldPosition, 600, 5_000,
      () -> {
        if (!canExecute()) return true;

        ammunitionCube.set(getAmmunitionCube(island));
        if (!longTapped.get() && ammunitionCube.get() != null &&
          script.getFinger().tapGameScreen(ammunitionCube.get(), "Fill")) {
          longTapped.set(true);
        }
        return false;
      });

    if (!canExecute()) return false;
    return result;
  }

  private Polygon getAmmunitionCube(Island island) {
    WorldPosition ammunition = AMMUNITION.get(island);
    if (ammunition == null) return null;

    Polygon cube = script.getSceneProjector().getTileCube(ammunition, 70, true);
    if (cube == null) return null;
    cube = cube.getResized(0.8);
    if (!script.getWidgetManager().insideGameScreen(cube, List.of())) return null;

    drawHelper.drawPolygon("ammunition-cube", cube, Color.CYAN);
    return cube;
  }
}

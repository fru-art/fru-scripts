import com.osmb.api.location.Location3D;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.visual.PixelAnalyzer;
import helper.DrawHelper;
import helper.EntityHelper;
import helper.InventoryHelper;
import helper.WaitHelper;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class MineTask extends Task {
  private static final int FAILED_ATTEMPTS_HOP_THRESHOLD = 6;

  private final DrawHelper drawHelper;
  private final EntityHelper entityHelper;
  private final InventoryHelper inventoryHelper;
  private final List<String> mineableRockNames;
  private final Set<Integer> mineables;
  private final WaitHelper waitHelper;

  private int consecutiveFailedAttempts = 0;

  public MineTask(Script script, BeginnerPowerMinerScriptOptions scriptOptions) {
    super(script);

    mineableRockNames = scriptOptions.mineableRockNames;
    mineables = scriptOptions.mineables;

    drawHelper = new DrawHelper(script);
    entityHelper = new EntityHelper(script);
    inventoryHelper = new InventoryHelper(script, mineables);
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    return !this.inventoryHelper.getSnapshot().isFull();
  }

  @Override
  public boolean execute() {
    boolean executeResult = executeInternal();

    if (executeResult) {
      consecutiveFailedAttempts = 0;
    } else {
      consecutiveFailedAttempts++;
      String countDescriptor = consecutiveFailedAttempts == 1 ?
        "1st" :
        consecutiveFailedAttempts == 2 ?
          "2nd" :
          consecutiveFailedAttempts == 3 ?
            "3rd" :
            consecutiveFailedAttempts + "th";

      script.log(getClass(), "Failed to mine an item for the " + countDescriptor + " time");
    }

    if (consecutiveFailedAttempts >= FAILED_ATTEMPTS_HOP_THRESHOLD && script.canHopWorlds()) {
      script.log(getClass(), "Hopping worlds due to too many failed attempts");
      script.getProfileManager().forceHop();
      consecutiveFailedAttempts = 0;
    }

    return executeResult;
  }

  private boolean executeInternal() {
    List<RSObject> mineableRocks = getMineableRocks(mineableRockNames);
    mineableRocks.forEach(
      rock -> drawHelper.drawPosition("mineableRock", rock.getWorldPosition(), Color.GREEN));

    if (mineableRocks.isEmpty()) return false;
    RSObject rockToMine = mineableRocks.get(0);
    if (rockToMine == null) return false;

    LocalPosition rockToMinePosition = rockToMine.getLocalPosition();
    script.log(getClass(),
      "Walking to closest mineable rock: " + rockToMinePosition.getX() + " " + rockToMinePosition.getY());

    script.getFinger().tapGameScreen(rockToMine.getConvexHull());

    double initialTileDistance = rockToMine.getTileDistance(script.getWorldPosition());
    // Wait for player to move
    if (!script.submitHumanTask(() -> !entityHelper.isPlayerIdling(), 3_000)) return false;
    // Wait for player to reach rock
    if (!script.submitHumanTask(() -> rockToMine.getTileDistance(script.getWorldPosition()) == 1,
      (int) initialTileDistance * 500))
      return false;

    script.log(getClass(), "Waiting for mining to complete");
    int initialMineablesCount = inventoryHelper.getSnapshot().getAmount(mineables);
    // Wait for player animation to complete
    boolean didMiningComplete = waitHelper.waitForNoChange(
      "Mining",
      entityHelper::isPlayerIdling,
      1_000,
      15_000, // Timeout after 15s; sometimes mining takes longer, but allow the script to retry
      () -> {
        // Early exit if obtained ore
        if (inventoryHelper.getSnapshot().getAmount(mineables) > initialMineablesCount) return true;
        // Early exit if rock mined by other player
        return !isVisibleRockSpawned(rockToMine);
      });
    if (!didMiningComplete) return false;

    // 'didMiningComplete' only checks for if the mining action was finished; the item could have been received by
    // another player
    return inventoryHelper.getSnapshot().getAmount(mineables) > initialMineablesCount;
  }

  private boolean isVisibleRockSpawned(RSObject rock) {
    Set<WorldPosition> visibleSpawnedRockPositions = getVisibleSpawnedRocks(rock.getName()).stream()
      .map(Location3D::getWorldPosition).collect(Collectors.toSet());
    return visibleSpawnedRockPositions.contains(rock.getWorldPosition());
  }

  /**
   * @return List of rocks with the given name that are interactable and on screen
   */
  private List<RSObject> getVisibleSpawnedRocks(List<String> rockNames) {
    List<RSObject> visibleRocks =
      entityHelper.getNamedObjects(rockNames).stream().filter(RSObject::isInteractableOnScreen).toList();
    return entityHelper
      .filterUnspawnedObjects(visibleRocks, PixelAnalyzer.RespawnCircleDrawType.CENTER, 25, 7);
  }
  private List<RSObject> getVisibleSpawnedRocks(String rockName) {
    return getVisibleSpawnedRocks(List.of(rockName));
  }

  /**
   * Rules for determining which rocks can be mined (in order for performance considerations):
   * 1. Visible and spawned i.e. interactable, on screen, and no respawn circle
   * 2. Vacant i.e. no other adjacent players
   * 3. Reachable
   */
  private List<RSObject> getMineableRocks(List<String> rockNames) {
    List<RSObject> visibleSpawnedRocks = getVisibleSpawnedRocks(rockNames);
    List<RSObject> spawnedRocks = entityHelper.filterUnspawnedObjects(
      visibleSpawnedRocks, PixelAnalyzer.RespawnCircleDrawType.CENTER, 25, 7);
    List<RSObject> vacantRocks = entityHelper.filterOccupiedObjects(
      ((BeginnerPowerMinerScript) script).scriptCore, spawnedRocks, true);

    // Filter reachable rocks and sort by distance
    WorldPosition position = script.getWorldPosition();
    Map<RSObject, Integer> vacantRockTileDistances = vacantRocks.stream().collect(Collectors.toMap(
      Function.identity(),
      rock -> rock.getTileDistance(position)
    ));

    return vacantRocks.stream()
      .filter(rock -> {
        Integer distance = vacantRockTileDistances.get(rock);
        return distance != null && distance != 0;
      })
      .sorted(Comparator.comparingInt(vacantRockTileDistances::get))
      .toList();
  }
}

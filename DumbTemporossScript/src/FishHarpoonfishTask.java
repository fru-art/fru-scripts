import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.WalkConfig;
import helper.DetectionHelper;
import helper.DrawHelper;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FishHarpoonfishTask extends Task {
  private static final Map<Island, Set<WorldPosition>> FISHING_POSITIONS = Map.of(
    Island.NORTH, Set.of(
      new RectangleArea(3051, 2994, 0, 3, 0),
      new RectangleArea(3048, 3001, 0, 1, 0),
      new RectangleArea(3036, 2996, 2, 0, 0))
        .stream()
        .flatMap(rectangleArea -> rectangleArea.getAllWorldPositions().stream())
        .collect(Collectors.toSet()),
    Island.SOUTH, Set.of(
        new RectangleArea(3035, 2963, 2, 0, 0),
        new RectangleArea(3046, 2954, 0, 1, 0),
        new RectangleArea(3048, 2957, 0, 0, 0))
      .stream()
      .flatMap(rectangleArea -> rectangleArea.getAllWorldPositions().stream())
      .collect(Collectors.toSet())
  );
  private static final Map<Island, WorldPosition> NEAR_FISHING_POSITIONS = Map.of(
    Island.NORTH, new WorldPosition(3048, 2997, 0),
    Island.SOUTH, new WorldPosition(3047, 2962, 0)
  );

  private final DumbTemporossScript script;

  private final DetectionHelper detectionHelper;
  private final DrawHelper drawHelper;
  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;

  private final SearchableImage harpoonfishImage;
  private final List<SearchableImage> harpoonfishQuarters;
  private final Random random;

  private Integer fishThreshold;

  public FishHarpoonfishTask(DumbTemporossScript script) {
    super(script);
    this.script = script;

    detectionHelper = new DetectionHelper(script);
    drawHelper = new DrawHelper(script);
    inventoryHelper = new InventoryHelper(script, Set.of(
      ItemID.HARPOON, ItemID.HARPOONFISH, ItemID.RAW_HARPOONFISH, ItemID.ROPE
    ));
    waitHelper = new WaitHelper(script);

    SearchableImage[] harpoonfishImages = script.getItemManager().getItem(ItemID.RAW_HARPOONFISH, true);
    harpoonfishImage = harpoonfishImages[harpoonfishImages.length - 1];
    harpoonfishQuarters = detectionHelper.getSearchableQuarters(harpoonfishImage);
    random = new Random();
  }

  @Override
  public boolean canExecute() {
    if (script.canExecuteInterruptTask()) return false;

    Island island = script.getIsland();
    if (island == null) return false;

    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    /*
     * Rules (any met) for skipping execution in favor of cooking and delivering
     * 1. Started cooking and rope missing
     * 2. Started cooking and have minimum fish
     * 3. On boat and have cooked fish
     */
    boolean canTurnInEarly = (snapshot.contains(ItemID.HARPOONFISH) && !snapshot.contains(ItemID.ROPE)) ||
      (snapshot.contains(ItemID.HARPOONFISH) &&
        snapshot.getAmount(ItemID.RAW_HARPOONFISH, ItemID.HARPOONFISH) >= DumbTemporossScript.MIN_FISH) ||
      (snapshot.contains(ItemID.HARPOONFISH) && script.isNearBoat());
    if (canTurnInEarly) return false;

    if (fishThreshold == null) fishThreshold = random.nextInt(DumbTemporossScript.MIN_FISH, DumbTemporossScript.MAX_FISH);
    return !snapshot.isFull() && snapshot.contains(ItemID.HARPOON) &&
      snapshot.getAmount(ItemID.RAW_HARPOONFISH) < fishThreshold;
  }

  @Override
  public boolean execute() {
    Island island = script.getIsland();
    assert island != null;
    Set<WorldPosition> fishingPositions = FISHING_POSITIONS.get(island);
    if (fishingPositions == null || fishingPositions.isEmpty()) return false;

    List<WorldPosition> activeFishingPositions = getActiveFishingPositions(fishingPositions);

    // If no active fishing spots found, try walking near fishing spots
    if (activeFishingPositions.isEmpty()) {
      WorldPosition nearFishingPosition = NEAR_FISHING_POSITIONS.get(island);
      if (nearFishingPosition == null) return false;

      WalkConfig walkConfig = new WalkConfig.Builder()
        .breakCondition(() -> !canExecute() || !getActiveFishingPositions(fishingPositions).isEmpty())
        .build();

      script.getWalker().walkTo(nearFishingPosition, walkConfig);
      if (!canExecute()) return true;
      activeFishingPositions = getActiveFishingPositions(fishingPositions);
      if (activeFishingPositions.isEmpty()) return false;
    }

    WorldPosition didTapFishingPosition = null;
    for (WorldPosition activeFishingPosition : activeFishingPositions) {
      if (!canExecute()) return true;

      Polygon polygon = script.getSceneProjector().getTilePoly(activeFishingPosition);
      if (polygon == null) continue;
      if (script.getFinger().tapGameScreen(polygon)) {
        didTapFishingPosition = activeFishingPosition;
        break;
      }
    }

    if (didTapFishingPosition == null) {
      script.log(getClass(), "Failed to tap on existing active fishing positions");
      return false;
    }

    WorldPosition finalDidTapFishingPosition = didTapFishingPosition;
    AtomicInteger rawHarpoonfishCount = new AtomicInteger(
      inventoryHelper.getSnapshot().getAmount(ItemID.RAW_HARPOONFISH));

    AtomicBoolean longTapped = new AtomicBoolean(false);
    boolean result = waitHelper.waitForNoChange(
      "position",
      script::getWorldPosition,
      600,
      (int) script.getWorldPosition().distanceTo(finalDidTapFishingPosition) * 1_500,
      () -> {
        if (!canExecute()) return true;
        if (!getActiveFishingPositions(fishingPositions).contains(finalDidTapFishingPosition)) return true;

        Polygon polygon = script.getSceneProjector().getTilePoly(finalDidTapFishingPosition);
        if (polygon != null && !longTapped.get() && script.getFinger().tapGameScreen(polygon, "Harpoon")) {
          longTapped.set(true);
        }

        WorldPosition position = script.getWorldPosition();
        return Math.abs(position.getX() - finalDidTapFishingPosition.getX()) +
          Math.abs(position.getY() - finalDidTapFishingPosition.getY()) < 1.05;
      });
    if (!canExecute()) return true;
    if (!getActiveFishingPositions(fishingPositions).contains(finalDidTapFishingPosition)) {
      script.log(getClass(), "Fishing spot moved before reaching it");
      return true;
    }
    if (!result) {
      script.log(getClass(), "Failed to reach active fishing position");
    }

    // Wait for end of fishing action
    AtomicBoolean exit = new AtomicBoolean(false);
    while (canExecute() && !exit.get()) {
      result = script.submitTask(() -> {
        if (!canExecute()) return true;
        if (!getActiveFishingPositions(fishingPositions).contains(finalDidTapFishingPosition)) {
          script.log(getClass(), "Fishing spot moved");
          exit.set(true);
          return true;
        }

        int nextRawHarpoonfishCount = inventoryHelper.getSnapshot().getAmount(ItemID.RAW_HARPOONFISH);

        if (nextRawHarpoonfishCount == rawHarpoonfishCount.get()) return false;
        if (nextRawHarpoonfishCount < rawHarpoonfishCount.get()) {
          script.log(getClass(), "Lost fish for an unknown reason");
          exit.set(true);
          return true;
        }

        rawHarpoonfishCount.set(nextRawHarpoonfishCount);
        return true;
      }, 6_600); // 95% confidence to catch a fish within 11 ticks

      if (!result) exit.set(true);
    }

    fishThreshold = null;
    return true;
  }

  private List<WorldPosition> getActiveFishingPositions(Set<WorldPosition> fishingPositionsSet) {
    WorldPosition playerPosition = script.getWorldPosition();

    List<WorldPosition> fishingPositions = fishingPositionsSet.stream().toList();
    List<Rectangle> itemSearchBoundss = fishingPositions.stream()
      .map(fishingPosition -> {
        Point centerPoint = script.getSceneProjector().getTilePoint(fishingPosition, null, 40);
        return new Rectangle(
          centerPoint.x - harpoonfishImage.width / 2,
          centerPoint.y - harpoonfishImage.height / 2 - 20,
          harpoonfishImage.width,
          harpoonfishImage.height
        );
      }).toList();
    drawHelper.drawRectangles("item-search-bounds", itemSearchBoundss, Color.WHITE);

    List<WorldPosition> activeFishingPositions = new ArrayList<>();
    for (int i = 0; i < fishingPositions.size(); i++) {
      WorldPosition fishingPosition = fishingPositions.get(i);
      Rectangle itemSearchBounds = itemSearchBoundss.get(i);
      assert fishingPosition != null;
      assert itemSearchBounds != null;

      ImageSearchResult result = script.getImageAnalyzer().findLocation(
        itemSearchBounds, harpoonfishQuarters.toArray(new SearchableImage[0]));
      if (result != null) activeFishingPositions.add(fishingPosition);
    }

    activeFishingPositions = activeFishingPositions.stream()
      .sorted(Comparator.comparingDouble(fishingPosition -> fishingPosition.distanceTo(playerPosition)))
      .toList();

    drawHelper.drawPositions("active-fishing-positions", activeFishingPositions, Color.GREEN);
    return activeFishingPositions;
  }
}

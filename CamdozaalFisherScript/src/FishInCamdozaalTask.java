import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.visual.ImageAnalyzer;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.pathing.CollisionManager;
import helper.EntityHelper;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FishInCamdozaalTask extends Task {
  private static final List<WorldPosition> FISHING_POSITIONS =
    List.copyOf(new RectangleArea(2926, 5776, 2, 8, 0).getAllWorldPositions());
  private static final int TILE_HEIGHT = 15;


  /**
   * We have two detection images for the item tiles, one for the top half and one for the bottom half. This is because
   * when zoomed out adjacent tile images can overlap.
   */
  private final List<SearchableImage> tileImages = new ArrayList<>();

  private final EntityHelper entityHelper;
  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;

  public FishInCamdozaalTask(Script script) {
    super(script);
    entityHelper = new EntityHelper(script);
    inventoryHelper = new InventoryHelper(script, Collections.emptySet());
    waitHelper = new WaitHelper(script);

    addTileImages(ItemID.RAW_TETRA);
    addTileImages(ItemID.RAW_CATFISH);
    addTileImages(ItemID.RAW_GUPPY);
    addTileImages(ItemID.RAW_CAVEFISH);
  }

  public boolean canExecute() {
    // TODO: Check for fishing net but inventory detection for that item doesn't work right now
    return !inventoryHelper.getSnapshot().isFull();
  }

  public boolean execute() {
    List<WorldPosition> fishingSpots = getFishingSpots();
    if (fishingSpots.isEmpty()) {
      script.log(getClass(), "Failed to find fishing spots");
      return false;
    }

    // Start fishing
    script.log(getClass(), "Starting fishing");
    WorldPosition closestFishingSpot = script.getWorldPosition().getClosest(fishingSpots);
    double distanceToFishingSpot = script.getWorldPosition().distanceTo(closestFishingSpot);
    Polygon tilePoly = script.getSceneProjector().getTilePoly(closestFishingSpot);
    script.getFinger().tapGameScreen(tilePoly);

    if (!script.submitHumanTask(
      () -> CollisionManager.isCardinallyAdjacent(script.getWorldPosition(), closestFishingSpot),
      (int) (distanceToFishingSpot * 1_000),
      false,
      true)) {
      script.log(getClass(), "Failed to reach fishing spot");
      return false;
    }

    // Wait for fishing to stop
    // - Fishing one fish should take less than 20s
    // - Fishing spots last for a maximum of 5 minutes
    script.log(getClass(), "Waiting for fishing to complete");
    return waitHelper.waitForNoChange(
      "Fishing",
      entityHelper::isPlayerIdling,
      5_000,
      // Each iteration of fishing animation should not take more than 5s
      5 * 60_000,
      // Fishing a full inventory should not take more than 5m
      () -> {
        // Inventory full
        if (inventoryHelper.getSnapshot().isFull()) {
          script.log(getClass(), "Fishing completed due to inventory full");
          return true;
        }
        // Fishing spot disappeared
        if (!CollisionManager.isCardinallyAdjacent(script.getWorldPosition(), closestFishingSpot)) {
          script.log(getClass(), "Fishing completed due to fishing spot disappeared");
          return true;
        }
        return false;
      });
  }

  private void addTileImages(int itemId) {
    SearchableImage[] itemImages = script.getItemManager().getItem(itemId, true);
    SearchableImage imageTop = itemImages[itemImages.length - 1];
    SearchableImage imageBottom =
      new SearchableImage(imageTop.copy(), imageTop.getToleranceComparator(), imageTop.getColorModel());
    makeHalfTransparent(imageTop, true);
    makeHalfTransparent(imageBottom, false);
    tileImages.add(imageTop);
    tileImages.add(imageBottom);
  }

  private List<WorldPosition> getFishingSpots() {
    List<WorldPosition> fishingSpots = new ArrayList<>();

    for (WorldPosition fishingSpot : FISHING_POSITIONS) {
      for (SearchableImage tileImage : tileImages) {
        if (!checkForTileItem(fishingSpot, tileImage)) continue;

        fishingSpots.add(fishingSpot);
        // Draw fishing spot
        script.getScreen().queueCanvasDrawable(
          "foundFishingSpot=" + fishingSpot, canvas -> {
            Polygon tilePoly = script.getSceneProjector().getTilePoly(fishingSpot);
            canvas.fillPolygon(tilePoly, Color.GREEN.getRGB(), 0.3);
            canvas.drawPolygon(tilePoly, Color.GREEN.getRGB(), 1);
          });
        break;
      }
    }

    return fishingSpots;
  }

  private static void makeHalfTransparent(SearchableImage image, boolean topHalf) {
    int startY = topHalf ? 0 : image.getHeight() / 2;
    int endY = topHalf ? image.getHeight() / 2 : image.getHeight();
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = startY; y < endY; y++) {
        image.setRGB(x, y, ColorUtils.TRANSPARENT_PIXEL);
      }
    }
  }

  private boolean checkForTileItem(WorldPosition tilePosition, SearchableImage itemImage) {
    Point point = script.getSceneProjector().getTilePoint(tilePosition, /* Center point */ null, TILE_HEIGHT);
    if (point == null) {
      script.log(getClass(), "No tile point found for position: " + tilePosition);
      return false;
    }

    Point tileItemPoint = new Point(point.x - (itemImage.width / 2), point.y - (itemImage.height / 2) - 20);
    int radius = 6;

    com.osmb.api.shape.Rectangle tileItemArea = new Rectangle(
      tileItemPoint.x - radius,
      tileItemPoint.y - radius,
      itemImage.height + (radius * 2),
      itemImage.height + (radius * 2));
    ImageAnalyzer imageAnalyzer = script.getImageAnalyzer();
    if (imageAnalyzer == null) return false;

    ImageSearchResult result = imageAnalyzer.findLocation(tileItemArea, itemImage);
    boolean found = result != null;

    script.getScreen().queueCanvasDrawable(
      "tilePosition=" + tilePosition, canvas -> {
        if (found) canvas.fillRect(tileItemArea, Color.GREEN.getRGB(), 0.3);
        canvas.drawRect(
          tileItemArea,
          found ? Color.GREEN.getRGB() : Color.WHITE.getRGB(),
          1);
      });

    if (found) {
      script.log(getClass(), "Found " + result.getScore());
      return true;
    }

    return false;
  }
}

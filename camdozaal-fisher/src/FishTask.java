import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.pathing.CollisionManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FishTask extends Task {
  // Right bound: 2931, Top bound: 5783
  private static final RectangleArea BRIDGE_AREA = new RectangleArea(2930, 5779, 1, 4, 0);
  private static final List<WorldPosition> BRIDGE_POSITIONS = List.copyOf(BRIDGE_AREA.getAllWorldPositions());
  // Right bound: 2928, Top bound: 5784
  private static final List<WorldPosition> FISHING_POSITIONS =
    List.copyOf(new RectangleArea(2926, 5776, 2, 8, 0).getAllWorldPositions());

  private final Random random = new Random();
  private final ScriptHelpers scriptHelpers;
  /**
   * We have two detection images for the tetra tile, one for the top half and one for the bottom half. This is because
   * when zoomed out adjacent tile images can overlap.
   */
  private final SearchableImage tetraTileImageTop;
  private final SearchableImage tetraTileImageBottom;

  public FishTask(Script script) {
    super(script);
    this.scriptHelpers = new ScriptHelpers(script);

    // Initialize tetra tile detection images
    SearchableImage[] itemImages = script.getItemManager().getItem(ItemID.RAW_TETRA, true);
    tetraTileImageTop = itemImages[itemImages.length - 1];
    tetraTileImageBottom = new SearchableImage(tetraTileImageTop.copy(), tetraTileImageTop.getToleranceComparator(), tetraTileImageTop.getColorModel());
    makeHalfTransparent(tetraTileImageTop, true);
    makeHalfTransparent(tetraTileImageBottom, false);
  }

  public boolean canExecute() {
    // TODO: Check for fishing net but inventory detection for that item doesn't work right now
    return !script.getWidgetManager().getInventory().search(Collections.emptySet()).isFull();
  }

  public boolean execute() {
    List<WorldPosition> fishingSpots = getFishingSpots();

    int distanceToBridge = BRIDGE_AREA.distanceTo(script.getWorldPosition());
    if (fishingSpots.isEmpty()) {
      if (distanceToBridge < 1) {
        script.log(getClass(), "Failed to find fishing spots");
        return false;
      } else {
        // Walk to bridge if no visible fishing spots
        script.log(getClass(), "Walking to fishing area");
        WorldPosition bridgePosition = BRIDGE_POSITIONS.get(random.nextInt(BRIDGE_POSITIONS.size()));
        int distanceToBridgePosition = bridgePosition.distanceTo(script.getWorldPosition());
        script.getWalker().walkTo(bridgePosition);

        if (!script.submitHumanTask(
          () -> bridgePosition.distanceTo(script.getWorldPosition()) < 1,
          distanceToBridgePosition * 1000)) {
          script.log(getClass(), "Failed to walk to fishing area");
        }

        fishingSpots = getFishingSpots();
        if (fishingSpots.isEmpty()) {
          script.log(getClass(), "Failed to find fishing spots twice");
          return false;
        }
      }
    }

    // Start fishing
    script.log(getClass(), "Starting fishing");
    WorldPosition closestFishingSpot = script.getWorldPosition().getClosest(fishingSpots);
    int distanceToFishingSpot = script.getWorldPosition().distanceTo(closestFishingSpot);
    Polygon tilePoly = script.getSceneProjector().getTilePoly(closestFishingSpot);
    script.getFinger().tap(tilePoly);

    if (!script.submitHumanTask(
      () -> CollisionManager.isCardinallyAdjacent(script.getWorldPosition(), closestFishingSpot),
      distanceToFishingSpot * 1000)) {
        script.log(getClass(), "Failed to reach fishing spot");
        return false;
    }

    // Wait for fishing to stop
    // - Fishing one fish should take less than 20s
    // - Fishing spots last for a maximum of 5 minutes
    script.log(getClass(), "Waiting for fishing to complete");
    return scriptHelpers.waitForNoXp(20_000, 5 * 60_000, () -> {
      // Inventory full
      if (script.getWidgetManager().getInventory().search(Collections.emptySet()).isFull()) {
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

  private List<WorldPosition> getFishingSpots() {
    List<WorldPosition> fishingSpots = new ArrayList<>();

    for (WorldPosition fishingSpot : FISHING_POSITIONS) {
      if (scriptHelpers.checkForTileItem(fishingSpot, tetraTileImageBottom) ||
        scriptHelpers.checkForTileItem(fishingSpot, tetraTileImageTop)) {
        fishingSpots.add(fishingSpot);

        // Draw fishing spot
        script.getScreen().queueCanvasDrawable("foundFishingSpot=" + fishingSpot, canvas -> {
          Polygon tilePoly = script.getSceneProjector().getTilePoly(fishingSpot);
          canvas.fillPolygon(tilePoly, Color.GREEN.getRGB(), 0.3);
          canvas.drawPolygon(tilePoly, Color.BLUE.getRGB(), 1);
        });
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
}

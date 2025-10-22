import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import helper.InventoryHelper;
import helper.WaitHelper;
import task.Task;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TakeFindsTask extends Task {
  private final ArtefactCleanerScript script;

  private final InventoryHelper inventoryHelper;
  private final WaitHelper waitHelper;

  private final Random random = new Random();

  public TakeFindsTask(ArtefactCleanerScript script) {
    super(script);
    this.script = script;

    inventoryHelper = new InventoryHelper(script, Stream.concat(
      ArtefactCleanerScript.TOOLS.stream(),
      Stream.of(ItemID.UNCLEANED_FIND)
    ).collect(Collectors.toSet()));
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && !snapshot.isFull();
  }

  @Override
  public boolean execute() {
    RSObject rocks = script.getObjectManager().getClosestObject(script.getWorldPosition(), "Dig Site specimen rocks");
    if (rocks == null) {
      script.log(getClass(), "Failed to find rocks");
      return false;
    }

    int initialDistance = rocks.getTileDistance(script.getWorldPosition());

    if (!rocks.interact("Take")) {
      script.log(getClass(), "Failed to interact with rocks");
      return false;
    }

    waitHelper.waitForNoChange("position",
      script::getWorldPosition,
      600,
      initialDistance * 600 + 600);

    if (script.scriptOptions.spamGatherRocksCheckbox.isSelected()) {
      // Spam until inventory full, check periodically
      while (true) {
        ItemGroupResult snapshot = inventoryHelper.getSnapshot();
        if (snapshot.isFull()) return true;

        AtomicBoolean first = new AtomicBoolean(true);
        script.pollFramesUntil(() -> {
          if (rocks.isInteractableOnScreen()) {
            Point gaussianPoint = getGaussianPoint(rocks.getConvexHull().getResized(1.1));
            int targetX = (int) gaussianPoint.getX();
            int targetY = (int) gaussianPoint.getY();
            if (!first.get()) {
//              script.pollFramesUntil(() -> false, random.nextInt(400, 700), true);
              script.pollFramesHuman(() -> false, 0, true);
            }
            first.set(false);
            script.getFinger().tapGameScreen(
              false,
              new Polygon(
                new int[]{ targetX - 1, targetX + 1, targetX + 1, targetX - 1 },
                new int[] { targetY - 1, targetY - 1, targetY + 1, targetY + 1 }));
          }
          ItemGroupResult lazySnapshot = inventoryHelper.getSnapshot(true);
          return lazySnapshot != null && lazySnapshot.isFull();
        }, snapshot.getFreeSlots() * 600 + 600, true);
      }
    } else {
      // Wait for auto-gather
      waitHelper.waitForNoChange("find-count",
        () -> inventoryHelper.getSnapshot().getAmount(ItemID.UNCLEANED_FIND),
        4 * 600 + 600,
        Integer.MAX_VALUE,
        () -> inventoryHelper.getSnapshot().isFull());

      return inventoryHelper.getSnapshot().isFull();
    }
  }

  private Point getGaussianPoint(Polygon polygon) {
    Rectangle bounds = polygon.getBounds();

    while (true) {
      // Mean is the rectangle center
      double meanX = bounds.getX() + bounds.getWidth() / 2.0;
      double meanY = bounds.getY() + bounds.getHeight() / 2.0;

      // Standard deviation ~ fraction of bounds size (tweakable)
      double stdDev = Math.min(bounds.getWidth(), bounds.getHeight()) / 6.0;

      // Generate circular Gaussian (Box-Muller transform)
      double u = random.nextDouble();
      double v = random.nextDouble();
      double radius = stdDev * Math.sqrt(-2.0 * Math.log(u));
      double theta = 2.0 * Math.PI * v;

      double offsetX = radius * Math.sin(theta);
      double offsetY = radius * Math.cos(theta);

      int clickX = (int) Math.round(meanX + offsetX);
      int clickY = (int) Math.round(meanY + offsetY);

      // Allow misclicks
//      if (polygon.contains(clickX, clickY)) {
        return new Point(clickX, clickY);
//      }
    }
  }
}

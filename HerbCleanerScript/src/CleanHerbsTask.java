import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.ui.tabs.Inventory;
import helper.InventoryHelper;
import task.Task;

import java.awt.*;
import java.util.Random;

public class CleanHerbsTask extends Task {
  private static final int[] STANDARD_PATTERN = {
    0, 1, 2, 3,
    7, 6, 5, 4,
    8, 9, 10, 11,
    15, 14, 13, 12,
    16, 17, 18, 19,
    23, 22, 21, 20,
    24, 25, 26, 27,
  };
  private final InventoryHelper inventoryHelper;
  private final Random random;

  public CleanHerbsTask(Script script) {
    super(script);

    inventoryHelper = new InventoryHelper(script, HerbCleanerScript.GRIMY_HERBS);
    random = new Random();
  }

  @Override
  public boolean canExecute() {
    return this.inventoryHelper.getSnapshot().containsAny(HerbCleanerScript.GRIMY_HERBS);
  }

  @Override
  public boolean execute() {
    Bank bank = script.getWidgetManager().getBank();
    if (bank != null && bank.isVisible()) bank.close();

    Inventory inventory = script.getWidgetManager().getInventory();
    if (inventory == null) return false;
    if (!inventory.isOpen()) inventory.open();

    for (int slot : STANDARD_PATTERN) {
      Rectangle slotBounds = inventory.getBoundsForSlot(slot).get();
      if (slotBounds == null) continue;
      Point point = getGaussianPoint(slotBounds);
      script.getFinger().tap(false, point.x, point.y);

      // Random AFK for up to 30s
      if (random.nextInt(1000) == 0) {
        script.submitHumanTask(() -> !this.inventoryHelper.getSnapshot().containsAny(HerbCleanerScript.GRIMY_HERBS),
          Integer.MAX_VALUE);
        script.submitHumanTask(() -> false, random.nextInt(30_000));
        break;
      }

      // Random AFK for up to 10s
      if (random.nextInt(1000) < 5) {
        script.submitHumanTask(() -> !this.inventoryHelper.getSnapshot().containsAny(HerbCleanerScript.GRIMY_HERBS),
          Integer.MAX_VALUE);
        script.submitHumanTask(() -> false, random.nextInt(10_000));
        break;
      }

      script.submitTask(() -> false, random.nextInt(50, 100));
    }

    script.submitHumanTask(() -> false, 0);
    return true;
  }

  private Point getGaussianPoint(Rectangle rect) {
    // Mean is the rectangle center
    double meanX = rect.getX() + rect.getWidth() / 2.0;
    double meanY = rect.getY() + rect.getHeight() / 2.0;

    // Standard deviation ~ fraction of rect size (tweakable)
    double stdDev = Math.min(rect.getWidth(), rect.getHeight()) / 6.0;

    // Generate circular Gaussian (Box-Muller transform)
    double u = random.nextDouble();
    double v = random.nextDouble();
    double radius = stdDev * Math.sqrt(-2.0 * Math.log(u));
    double theta = 2.0 * Math.PI * v;

    double offsetX = radius * Math.cos(theta);
    double offsetY = radius * Math.sin(theta);

    int clickX = (int) Math.round(meanX + offsetX);
    int clickY = (int) Math.round(meanY + offsetY);

    return new Point(clickX, clickY);
  }
}

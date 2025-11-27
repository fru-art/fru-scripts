import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import helper.InventoryHelper;
import helper.WaitHelper;
import java.awt.Point;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import task.Task;

public class TakeFindsTask
extends Task {
    private final ArtefactCleanerScript script;
    private final InventoryHelper inventoryHelper;
    private final WaitHelper waitHelper;
    private final Random random = new Random();

    public TakeFindsTask(ArtefactCleanerScript script) {
        super(script);
        this.script = script;
        this.inventoryHelper = new InventoryHelper((Script)script, Stream.concat(ArtefactCleanerScript.TOOLS.stream(), Stream.of(Integer.valueOf(11175))).collect(Collectors.toSet()));
        this.waitHelper = new WaitHelper(script);
    }

    @Override
    public boolean canExecute() {
        ItemGroupResult snapshot = this.inventoryHelper.getSnapshot();
        return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && !snapshot.isFull();
    }

    @Override
    public boolean execute() {
        RSObject rocks = this.script.getObjectManager().getClosestObject(this.script.getWorldPosition(), new String[]{"Dig Site specimen rocks"});
        if (rocks == null) {
            this.script.log(this.getClass(), "Failed to find rocks");
            return false;
        }
        int initialDistance = rocks.getTileDistance(this.script.getWorldPosition());
        if (!rocks.interact(new String[]{"Take"})) {
            this.script.log(this.getClass(), "Failed to interact with rocks");
            return false;
        }
        this.waitHelper.waitForNoChange("position", () -> ((ArtefactCleanerScript)this.script).getWorldPosition(), 600, initialDistance * 600 + 600);
        if (this.script.scriptOptions.spamGatherRocksCheckbox.isSelected()) {
            while (true) {
                ItemGroupResult snapshot;
                if ((snapshot = this.inventoryHelper.getSnapshot()).isFull()) {
                    return true;
                }
                AtomicBoolean first = new AtomicBoolean(true);
                this.script.pollFramesUntil(() -> {
                    ItemGroupResult lazySnapshot;
                    if (rocks.isInteractableOnScreen()) {
                        Point gaussianPoint = this.getGaussianPoint(rocks.getConvexHull().getResized(1.1));
                        int targetX = (int)gaussianPoint.getX();
                        int targetY = (int)gaussianPoint.getY();
                        if (!first.get()) {
                            this.script.pollFramesHuman(() -> false, 0, true);
                        }
                        first.set(false);
                        this.script.getFinger().tapGameScreen(false, (Shape)new Polygon(new int[]{targetX - 1, targetX + 1, targetX + 1, targetX - 1}, new int[]{targetY - 1, targetY - 1, targetY + 1, targetY + 1}));
                    }
                    return (lazySnapshot = this.inventoryHelper.getSnapshot(true)) != null && lazySnapshot.isFull();
                }, snapshot.getFreeSlots() * 600 + 600, true);
            }
        }
        this.waitHelper.waitForNoChange("find-count", () -> this.inventoryHelper.getSnapshot().getAmount(new int[]{11175}), 3000, Integer.MAX_VALUE, () -> this.inventoryHelper.getSnapshot().isFull());
        return this.inventoryHelper.getSnapshot().isFull();
    }

    private Point getGaussianPoint(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        double meanX = (double)bounds.getX() + (double)bounds.getWidth() / 2.0;
        double meanY = (double)bounds.getY() + (double)bounds.getHeight() / 2.0;
        double stdDev = (double)Math.min(bounds.getWidth(), bounds.getHeight()) / 6.0;
        double u = this.random.nextDouble();
        double v = this.random.nextDouble();
        double radius = stdDev * Math.sqrt(-2.0 * Math.log(u));
        double theta = Math.PI * 2 * v;
        double offsetX = radius * Math.sin(theta);
        double offsetY = radius * Math.cos(theta);
        int clickX = (int)Math.round(meanX + offsetX);
        int clickY = (int)Math.round(meanY + offsetY);
        return new Point(clickX, clickY);
    }
}

import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.minimap.Minimap;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.walker.WalkConfig;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class StealFromStallJob extends Job<ToolkitScript> {
  private final Set<Integer> items;
  private final PixelCluster.ClusterQuery itemQuery;
  private final double minItemSize;
  private final RectangleArea playerCheckArea;
  private final WorldPosition safespot;
  private final WorldPosition stallPosition;
  private final int stallRespawnTime;
  private final double stallTapBoundsModifier;
  private final RectangleArea startArea;

  private final Random random = new Random();

  public StealFromStallJob(ToolkitScript script,
                           WorldPosition stallPosition,
                           int stallRespawnTime,
                           double stallTapBoundsModifier,
                           WorldPosition safespot,
                           RectangleArea startArea,
                           RectangleArea playerCheckArea,
                           Set<Integer> items,
                           PixelCluster.ClusterQuery itemQuery,
                           double minItemSize) {
    super(script);

    this.items = items;
    this.itemQuery = itemQuery;
    this.minItemSize = minItemSize;
    this.playerCheckArea = playerCheckArea;
    this.safespot = safespot;
    this.stallRespawnTime = stallRespawnTime;
    this.stallPosition = stallPosition;
    this.stallTapBoundsModifier = stallTapBoundsModifier;
    this.startArea = startArea;
  }

  @Override
  public boolean canExecute() {
    if (!startArea.contains(script.getWorldPosition())) {
      script.log(
        getClass(),
        "Failed to start in correct area. Try getting closer to the correct stall. Stopping script...");
      script.stop();
      return false;
    }

    if (script.pollFramesUntilInventoryVisible(items).isFull()) {
      script.log(getClass(), "Skipped because inventory is full");
      return false;
    }

    return true;
  }

  @Override
  public boolean execute() {
    WalkConfig walkConfig = new WalkConfig.Builder().breakDistance(0).build();
    if (!script.getWorldPosition().equals(safespot) && !script.getWalker().walkTo(safespot, walkConfig)) {
      script.log(getClass(), "Failed to walk to safespot. Hopping worlds...");
      script.getProfileManager().forceHop(); // Hopping worlds may avoid obstacles like large item stacks
      return false;
    }

    // Sometimes the walk function exits a bit early
    script.pollFramesUntil(() -> script.getWorldPosition().distanceTo(safespot) < 0.1, 1_800);

    RSObject stall = script.getObjectManager().getRSObject(
      object -> object.getObjectArea().contains(stallPosition) && object.getName().contains("tall"));
    if (stall == null) {
      script.log(getClass(), "Failed to find stall");
      return false;
    }

    Minimap minimap = script.getWidgetManager().getMinimap();
    if (minimap == null) {
      script.log(getClass(), "Failed to get minimap");
      return false;
    }

    List<WorldPosition> players = minimap.getPlayerPositions().asList();
    for (WorldPosition player : players) {
      if (playerCheckArea.contains(player)) {
        script.log(getClass(), "Failed to find unoccupied stall. Attempting to hop worlds...");
        script.getProfileManager().forceHop();
        return false;
      }
    }

    Polygon stallHull = stall.getConvexHull();
    if (stallHull == null) {
      script.log(getClass(), "Failed to get stall hull");
      return false;
    }

    AtomicReference<Optional<PixelCluster>> itemClusterRef = new AtomicReference<>(getItemCluster(stallHull));
    BooleanSupplier checkItemCluster = () -> {
      itemClusterRef.set(getItemCluster(stallHull));
      script.queuePolygonDrawable(stallPosition.toString(), stallHull, itemClusterRef.get().isPresent() ? Color.GREEN : Color.RED);
      return itemClusterRef.get().isPresent();
    };
    int checkItemClusterTimeout = stallRespawnTime + 600;

    boolean foundItemCluster = random.nextInt(100) < 40 ?
      script.pollFramesHuman(checkItemCluster, checkItemClusterTimeout) :
      script.pollFramesUntil(checkItemCluster, checkItemClusterTimeout);
    if (!foundItemCluster) {
      script.log(getClass(), "Failed to ultimately find item cluster");
      return false;
    }

    assert itemClusterRef.get().isPresent();

    int initialItemsCount = script.pollFramesUntilInventoryVisible(items).getAmount(items);
    script.getFinger().tapGameScreen(stall.getConvexHull().getResized(stallTapBoundsModifier));

    boolean receivedItems = script.pollFramesUntil(() -> {
      int itemsCount = script.pollFramesUntilInventoryVisible(items).getAmount(items);
      return itemsCount > initialItemsCount;
    }, 1_800);
    if (!receivedItems) {
      script.log(getClass(), "Failed to receive items from stall");
      return false;
    }

    return true;
  }

  private Optional<PixelCluster> getItemCluster(Polygon stallHull) {
    Optional<PixelCluster> itemCluster = script.findLargestCluster(stallHull, itemQuery);
    if (itemCluster.isEmpty()) {
      script.log(getClass(), "Failed to find a potential item cluster");
      return Optional.empty();
    }

    double itemClusterBoundsSize = getBoundsSize(itemCluster.get().getBounds());
    double stallHullBoundsSize = getBoundsSize(stallHull.getBounds());
    double sizeRatio = itemClusterBoundsSize / stallHullBoundsSize;
    if (itemClusterBoundsSize / stallHullBoundsSize < minItemSize) {
      script.log(getClass(), "Failed to find large enough item cluster. Size: " + sizeRatio);
      return Optional.empty();
    }

    return itemCluster;
  }

  private int getBoundsSize(Rectangle bounds) {
    return bounds.width * bounds.height;
  }
}

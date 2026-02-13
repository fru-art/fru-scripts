import com.osmb.api.input.MenuEntry;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.minimap.Minimap;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.utils.Paint;

import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PickFlaxJob extends Job<FlaxPickerScript> {
  private static final SearchablePixel[] FLAX_PIXELS = new SearchablePixel[]{
    // The teal flax bud
    new SearchablePixel(-6820367, new SingleThresholdComparator(15), ColorModel.HSL),
    // The dark green plant
    new SearchablePixel(-15837167, new ChannelThresholdComparator(15, 15, 50), ColorModel.HSL),};

  private static final PolyArea SEERS_VILLAGE_PEN = new PolyArea(List.of(
    new WorldPosition(2744, 3452, 0),
    new WorldPosition(2743, 3452, 0),
    new WorldPosition(2742, 3453, 0),
    new WorldPosition(2741, 3453, 0),
    new WorldPosition(2740, 3452, 0),
    new WorldPosition(2739, 3451, 0),
    new WorldPosition(2738, 3451, 0),
    new WorldPosition(2736, 3449, 0),
    new WorldPosition(2736, 3446, 0),
    new WorldPosition(2737, 3445, 0),
    new WorldPosition(2737, 3439, 0),
    new WorldPosition(2739, 3437, 0),
    new WorldPosition(2741, 3437, 0),
    new WorldPosition(2742, 3436, 0),
    new WorldPosition(2745, 3436, 0),
    new WorldPosition(2746, 3437, 0),
    new WorldPosition(2751, 3437, 0),
    new WorldPosition(2751, 3451, 0),
    new WorldPosition(2745, 3451, 0)));

  private final Consumer<Canvas> paintListener = this::paint;

  public PickFlaxJob(FlaxPickerScript script) {
    super(script);
    // The script only stores weak references to listeners; make sure to save your listener somewhere or else it will
    // be automatically cleaned up
    script.addPaintListener(paintListener);
  }

  @Override
  public boolean canExecute() {
    return !script.pollFramesUntilInventoryVisible().isFull();
  }

  @Override
  public boolean execute() {
    while (!script.pollFramesUntilInventoryVisible().isFull()) {
      // 1. Walk to the flax area
      if (!walkToFlaxArea()) return false;

      // 2. Query flax plants
      List<RSObject> flaxPlants = getFlaxPlants();

      // 3. Keep trying nearby flax plants until one is successfully picked, then break to loop and re-query flax plants
      for (RSObject flaxPlant : flaxPlants) {
        if (pickFlax(flaxPlant)) break;
      }
    }

    return true;
  }

  private void paint(Canvas canvas) {
    // Skip passing canvas if the debug checkbox isn't selected, effectively skipping all drawings
    getFlaxPlants(script.options.debugCheckBox.isSelected() ? canvas : null);
  }

  private boolean pickFlax(RSObject flaxPlant) {
    boolean didPickAtLeastOneFlax = false;

    while (!script.pollFramesUntilInventoryVisible().isFull()) {
      Polygon hull = flaxPlant.getConvexHull();
      if (hull == null) return didPickAtLeastOneFlax;

      MenuEntry response = script.getFinger().tapGetResponse(true, hull);
      if (!response.getAction().equalsIgnoreCase("Pick")) return didPickAtLeastOneFlax;
      didPickAtLeastOneFlax = true;
    }

    return true; // Return true if inventory filled
  }

  private List<RSObject> getFlaxPlants() { return getFlaxPlants(null); }
  private List<RSObject> getFlaxPlants(Canvas canvas) {
    Minimap minimap = script.getWidgetManager().getMinimap();
    WorldPosition position = script.getWorldPosition();
    List<WorldPosition> positionsToAvoid =
      Stream.concat(minimap.getNPCPositions().asList().stream(), minimap.getPlayerPositions().asList().stream())
        .toList();

    Optional<Area> maybeTargetArea = getFlaxArea();

    // Return matching objects
    // Tip: Having multiple heuristics will make your script more performant; do your computation-heavy filtering last
    return script.getObjectManager().getObjects(object -> {
        // Filter out non-flax objects
        if (object.getName() == null || !object.getName().equalsIgnoreCase("Flax")) return false;

        // Filter out objects not in the target area
        if (maybeTargetArea.isPresent() && !maybeTargetArea.get().contains(object.getWorldPosition())) return false;

        // Filter out objects without a clear hull e.g. they are off the screen
        Polygon hull = object.getConvexHull();
        if (hull == null) return false;

        // Filter out objects near NPCs and players
        for (WorldPosition positionToAvoid : positionsToAvoid) {
          if (object.distance(positionToAvoid) < 3) {
            if (canvas != null) Paint.drawPolygon(canvas, hull, Color.ORANGE);
            return false;
          }
        }

        // Filter out objects that don't have a flax cluster or the cluster is too small
        // Tip: It's good to use ratios for determining if a cluster is "large enough" because camera zoom can make
        // pixel counts arbitrary
        Optional<PixelCluster> cluster = script.findLargestCluster(hull, FLAX_PIXELS, 5, 1);

        if (cluster.isEmpty()) {
          if (canvas != null) Paint.drawPolygon(canvas, hull, Color.RED);
          return false;
        }

        // Draw additional details about the pixel cluster if it is present but too small
        double pixelRatio = cluster.get().getPoints().size() / hull.area();
        if (pixelRatio < 0.05) {
          if (canvas != null) {
            Paint.drawPolygon(canvas, hull, Color.RED);
            Paint.drawCluster(canvas, cluster.get(), Color.RED);
            Point center = cluster.get().getCenter();
            Paint.drawSmallText(canvas, String.format("%.3f", pixelRatio), center.x, center.y, Color.WHITE, true);
          }
          return false;
        }

        if (canvas != null) Paint.drawPolygon(canvas, hull, Color.GREEN);
        return true;
      }).stream()
      // Sort by closest to player
      .sorted(Comparator.comparingDouble(object -> position == null ? 0 : object.distance(position))).toList();
  }

  private Optional<Area> getFlaxArea() {
    WorldPosition position = script.getWorldPosition();
    if (position == null) return Optional.empty();
    return position.getRegionID() == FlaxPickerScript.NEMUS_RETREAT ? Optional.empty() : Optional.of(SEERS_VILLAGE_PEN);
  }

  private boolean walkToFlaxArea() {
    Optional<Area> maybeTargetArea = getFlaxArea();
    if (maybeTargetArea.isEmpty()) return false;
    Area targetArea = maybeTargetArea.get();

    // 0. Exit early if already in the pen
    WorldPosition startPosition = script.getWorldPosition();
    if (startPosition != null && targetArea.contains(startPosition)) return true;

    // 1. Choose a random position to click towards for humanization
    WorldPosition targetPosition = targetArea.getRandomPosition();

    // 2. Configure breaking as soon as we enter the pen so we can start picking flax immediately
    WalkConfig walkConfig = new WalkConfig.Builder().breakCondition(() -> {
      WorldPosition position = script.getWorldPosition();
      if (position == null) return false;
      return targetArea.contains(position);
    }).build();

    // 3. Walk
    return script.getWalker().walkTo(targetPosition, walkConfig);
  }
}

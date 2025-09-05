import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.PixelCluster.ClusterQuery;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import helper.DetectionHelper;
import helper.DrawHelper;
import task.FirstMatchTaskScript;
import task.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ScriptDefinition(
  author = "fru",
  name = "Dumb Tempoross",
  description = "for completing the Tempoross minigame with help on dedicated worlds",
  skillCategory = SkillCategory.FISHING,
  version = 1.0
)
public class TemporossScript extends FirstMatchTaskScript {
  private static final SearchablePixel[] ESSENCE_PIXELS = {
    new SearchablePixel(-10888359, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
    new SearchablePixel(-16724992, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
  };
  public static final int MAX_FISH = 12;
  public static final int MIN_FISH = 7;
  private static final Map<Island, RectangleArea> NEAR_BOAT = Map.of(
    Island.NORTH, new RectangleArea(3032, 2973, 16, 18, 0),
    Island.SOUTH, new RectangleArea(3046, 2963, 15, 19, 0)
  );
  public static final int TEMPOROSS_COVE_REGION = 12078;

  private static final PixelCluster.ClusterQuery essenceBarQuery = new PixelCluster.ClusterQuery(1, 1024, Stream.concat(
    Stream.of(
      new SearchablePixel(-2401959, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
      new SearchablePixel(-3407872, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)
    ),
    Arrays.stream(ESSENCE_PIXELS)
  ).toArray(SearchablePixel[]::new));
  private static final PixelCluster.ClusterQuery essenceQuery = new PixelCluster.ClusterQuery(1, 0, ESSENCE_PIXELS);

  private final List<Task> interruptTasks;

  private final DetectionHelper detectionHelper;
  private final DrawHelper drawHelper;

  public TemporossScript(Object scriptCore) {
    super(scriptCore);
    interruptTasks = List.of(
      new StartMinigameTask(this),
      new UntetherTask(this),
      new TetherTask(this),
      new FishSpiritPoolTask(this)
    );

    detectionHelper = new DetectionHelper(this);
    drawHelper = new DrawHelper(this);
  }

//  @Override
//  public int poll() {
//    RSObject object = this.getObjectManager().getClosestObject("Ammunition crate");
//    this.log(object.toString());
//    return 0;
//  }

  @Override
  protected List<Task> getTaskList() {
    return Stream.concat(
      interruptTasks.stream(),
      Stream.of(
        new RetrieveItemsTask(this),
        new FishHarpoonfishTask(this),
        new CookHarpoonfishTask(this),
        new LoadHarpoonfishTask(this)
      )
    ).toList();
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(
      12332, 12588, // Sea Spirit Dock
      11566, 11567, 11569, 12587, // Transition region
      12078 // Tempoross Cove
    );
  }

  public boolean canExecuteInterruptTask() {
    for (Task task : interruptTasks) {
      if (task.canExecute()) {
        return true;
      }
    }

    return false;
  }

  public Island getIsland(WorldPosition position) {
    if (position.getRegionID() != TEMPOROSS_COVE_REGION) return null;

    return new RectangleArea(3004, 2983, 47, 24, 0).contains(position) ||
      new RectangleArea(3030, 2969, 12, 25, 0).contains(position) ? Island.NORTH : Island.SOUTH;
  }
  public Island getIsland() {
    return getIsland(this.getWorldPosition());
  }

  public boolean isNearBoat() {
    Island island = getIsland();
    if (island == null) return false;

    RectangleArea nearBoat = NEAR_BOAT.get(island);
    if (nearBoat == null) return false;

    WorldPosition position = this.getWorldPosition();
    return nearBoat.contains(position);
  }

  public Double getResource(ClusterQuery barQuery, ClusterQuery resourceQuery, Color color) {
    Rectangle screenBounds = this.getScreen().getBounds();
    Rectangle searchBounds = new Rectangle(
      screenBounds.getX(),
      screenBounds.getY(),
      screenBounds.getWidth() / 2,
      screenBounds.getHeight() / 2);

    Rectangle barBounds = detectionHelper.getFirstClusterBounds(searchBounds, barQuery);
    if (barBounds == null) return null;

    Rectangle resourceBounds = detectionHelper.getFirstClusterBounds(barBounds, resourceQuery);
    if (resourceBounds == null) return (double) 0;

    drawHelper.drawRectangle(barQuery.toString(), resourceBounds, color);

    return (double) resourceBounds.width / (double) barBounds.width;
  }

  public Double getEssence() {
    return getResource(essenceBarQuery, essenceQuery, Color.GREEN.darker());
  }

  public boolean isMinigameOver() {
    Island island = this.getIsland();
    if (island == null) return true;

    Double essence = getEssence();
    if (essence == null || essence > 0.05) return false;

    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    if (chatboxText != null) {
      for (String chatboxLine : chatboxText) {
        if (chatboxLine.contains("Tempoross retreats") || chatboxLine.contains("ferry you back")) {
          return true;
        }
      }
    }

    return false;
  }
}

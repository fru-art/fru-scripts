import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import task.TaskScript;

public class StealFromBakerTask extends StealFromStallTask {
  private static final PixelCluster.ClusterQuery CAKE_QUERY = new PixelCluster.ClusterQuery(
    2,
    5,
    new SearchablePixel[]{
      new SearchablePixel(-7573484, new ChannelThresholdComparator(5, 5, 20), ColorModel.HSL),
    });
  private static final WorldPosition SAFESPOT = new WorldPosition(2669, 3310, 0);
  private static final WorldPosition STALL = new WorldPosition(2667, 3311, 0);

  public StealFromBakerTask(TaskScript script) {
    super(
      script,
      STALL,
      2_400,
      0.65,
      SAFESPOT,
      new RectangleArea(2652, 3295, 20, 23, 0),
      new RectangleArea(2664, 3307, 7, 6, 0),
      BeginnerStallThiever.BAKED_GOODS,
      CAKE_QUERY,
      0.002);
  }
}

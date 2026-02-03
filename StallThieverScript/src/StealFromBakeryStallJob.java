import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmbtoolkit.script.ToolkitScript;

public class StealFromBakeryStallJob extends StealFromStallJob {
  private static final PixelCluster.ClusterQuery CAKE_QUERY = new PixelCluster.ClusterQuery(
    2,
    5,
    new SearchablePixel[]{
      new SearchablePixel(-7573484, new ChannelThresholdComparator(5, 5, 20), ColorModel.HSL),
    });
  private static final WorldPosition SAFESPOT = new WorldPosition(2669, 3310, 0);
  private static final WorldPosition STALL = new WorldPosition(2667, 3311, 0);

  public StealFromBakeryStallJob(ToolkitScript script) {
    super(
      script,
      STALL,
      2_400,
      0.65,
      SAFESPOT,
      new RectangleArea(2642, 3277, 34, 42, 0),
      new RectangleArea(2664, 3307, 7, 6, 0),
      StallThieverScript.BAKED_GOODS,
      CAKE_QUERY,
      0.002);
  }
}

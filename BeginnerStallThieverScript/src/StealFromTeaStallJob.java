import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmbtoolkit.script.ToolkitScript;

public class StealFromTeaStallJob extends StealFromStallJob {
  public StealFromTeaStallJob(ToolkitScript script) {
    super(
      script,
      new WorldPosition(3270, 3410, 0),
      2_400,
      0.8,
      new WorldPosition(3268, 3410, 0),
      new RectangleArea(3241, 3408, 31, 21, 0),
      new RectangleArea(3264, 3408, 8, 7, 0),
      BeginnerStallThieverScript.TEA,
      new PixelCluster.ClusterQuery(
        1,
        5,
        new SearchablePixel[]{
          new SearchablePixel(-7437437, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
        }),
      0.02);
  }
}

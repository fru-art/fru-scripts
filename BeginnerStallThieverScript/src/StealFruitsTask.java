import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import task.TaskScript;

public class StealFruitsTask extends StealFromStallTask {
  public StealFruitsTask(TaskScript script) {
    super(
      script,
      new WorldPosition(1795, 3608, 0),
      2_400,
      0.8,
      new WorldPosition(1796, 3608, 0),
      new RectangleArea(1796, 3606, 4, 4, 0),
      new RectangleArea(1791, 3604, 14, 12, 0),
      BeginnerStallThieverScript.FRUITS,
      new PixelCluster.ClusterQuery(
        10,
        5,
        new SearchablePixel[]{
          new SearchablePixel(-8665062, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-11113926, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-4546670, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-4770260, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-6574313, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-2913510, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
          new SearchablePixel(-15568107, new ChannelThresholdComparator(5, 5, 15), ColorModel.HSL),
        }),
      0.2);
  }
}

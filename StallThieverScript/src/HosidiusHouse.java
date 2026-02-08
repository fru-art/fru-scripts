import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmbtoolkit.script.ToolkitScript;
import com.osmbtoolkit.utils.Direction;
import com.osmbtoolkit.utils.Door;

public class HosidiusHouse {
  private static final SearchablePixel[] DOOR_PIXELS = new SearchablePixel[] {
    new SearchablePixel(-11909820, new SingleThresholdComparator(10), ColorModel.HSL),
    new SearchablePixel(-7043981, new SingleThresholdComparator(10), ColorModel.HSL),
    new SearchablePixel(-14211803, new SingleThresholdComparator(10), ColorModel.HSL),
  };
  private static final RectangleArea HOUSE = new RectangleArea(1795, 3606, 6, 7, 0);
  private static final WorldPosition INSIDE_HOUSE = new WorldPosition(1796, 3608, 0);
  private static final WorldPosition OUTSIDE_HOUSE = new WorldPosition(1808, 3583, 0);

  private final Door houseDoor;
  private final Script script;

  public HosidiusHouse(ToolkitScript script) {
    this.houseDoor = new Door(
      script,
      new WorldPosition(1797.9, 3606, 0),
      Direction.PrimaryDirection.E,
      Direction.PrimaryDirection.S,
      DOOR_PIXELS,
      0.25,
      180,
      0
    );
    this.houseDoor.debug(true);
    this.script = script;
  }

  public boolean passThrough(boolean shouldEnter) {
    if (shouldEnter == isInside()) return true;

    WorldPosition destination = shouldEnter ? INSIDE_HOUSE : OUTSIDE_HOUSE;
    if (!houseDoor.passTo(destination, () -> shouldEnter == isInside())) {
      script.log(getClass(), "Failed to pass house, shouldEnter: " + shouldEnter);
      return false;
    }

    return true;
  }

  private boolean isInside() {
    WorldPosition position = script.getWorldPosition();
    if (position == null) return false;
    return HOUSE.contains(position);
  }
}

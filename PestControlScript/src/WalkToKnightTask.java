import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.walker.WalkConfig;
import task.Task;
import task.TaskScript;

public class WalkToKnightTask extends Task {
  private static final int BREAK_DISTANCE = 3;
  private static final WorldPosition DESTINATION = new WorldPosition(2657, 2589, 0);

  public WalkToKnightTask(TaskScript script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    WorldPosition position = script.getWorldPosition();
    return position.getRegionID() == PestControlScript.MINIGAME_REGION &&
      !PestControlScript.NEAR_KNIGHT.contains(position);
  }

  @Override
  public boolean execute() {
    WalkConfig walkConfig = new WalkConfig.Builder().breakDistance(BREAK_DISTANCE).build();
    return script.getWalker().walkTo(DESTINATION, walkConfig);
  }
}

import com.osmb.api.ScriptCore;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.ObjectIdentifier;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ScriptDefinition(
  author = "fru",
  name = "Test",
  description = "A test script",
  skillCategory = SkillCategory.OTHER,
  version = 0.1
)

public class TestScript extends Script {
  private final ScriptCore scriptCore;

  public TestScript(Object scriptCore) {
    super(scriptCore);
    this.scriptCore = (ScriptCore) scriptCore;
  }

  @Override
  public int poll() {
//    WorldPosition myPosition = getWorldPosition();
    List<WorldPosition> playerPositions = getWidgetManager().getMinimap().getPlayerPositions().asList();
//    log(getClass(), "My position: "+ myPosition);
//    log(getClass(), "Player positions: " + playerPositions.size());
//    getScreen().queueCanvasDrawable("playPositions", (canvas) -> {
//      playerPositions.forEach((position) -> {
//        Polygon tilePoly = getSceneProjector().getTilePoly(position);
//        canvas.fillPolygon(tilePoly, Color.GREEN.getRGB(), 0.3);
//        canvas.drawPolygon(tilePoly, Color.BLUE.getRGB(), 1);
//      });
//    });
//
//    log(getClass(), "Player positions includes me: " + playerPositions.contains(myPosition));
//    log(getClass(), "NPC positions: " + getWidgetManager().getMinimap().getNPCPositions().size());


    List<RSObject> objects = getObjectManager().getObjects((object) -> {
      return object.getName() != null &&
        object.getName().compareToIgnoreCase("Tin rocks") == 0 &&
        object.getActions() != null;
    });
    log(getClass(), "Objects found: " + objects.size());

    getScreen().queueCanvasDrawable("tinOres", (canvas) -> {
      objects.forEach(object -> {
        object.getSurroundingPositions().forEach((position) -> {
          Polygon tilePoly = getSceneProjector().getTilePoly(position);
          boolean occupied = playerPositions.contains(position.toWorldPosition(scriptCore));
          canvas.fillPolygon(tilePoly, occupied ? Color.RED.getRGB() : Color.GREEN.getRGB(), 0.3);
          canvas.drawPolygon(tilePoly, Color.BLUE.getRGB(), 1);
        });
      });
    });

    return 0;
  }

  @Override
  public int[] regionsToPrioritise() {
    return new int[]{12596, 12849};
  }
}

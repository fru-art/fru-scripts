import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.SceneManager;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@FunctionalInterface
interface NpcMatcher {
  boolean test(WorldPosition npcPosition);
}

public class NpcHelper {
  private final Script script;

  private final DrawHelper drawHelper;

  public NpcHelper(Script script) {
    this.script = script;

    drawHelper = new DrawHelper(script);
  }

  public boolean getNpcStill(WorldPosition npc) {
    return Math.abs(npc.getPreciseX() - npc.getX()) < 0.1 &&
      Math.abs(npc.getPreciseY() - npc.getY()) < 0.1;
  }

  public NpcMatcher getUnoccupiedNpcMatcher() {
    Set<WorldPosition> players = new HashSet<>(
      script.getWidgetManager().getMinimap().getPlayerPositions().asList());

    return npc -> {
      int npcX = npc.getX();
      int npcY = npc.getY();
      int npcPlane =  npc.getPlane();

      WorldPosition north = new WorldPosition(npcX, npcY + 1, npcPlane);
      if (players.contains(north)) return false;
      WorldPosition east = new WorldPosition(npcX + 1, npcY, npcPlane);
      if (players.contains(east)) return false;
      WorldPosition south = new WorldPosition(npcX, npcY - 1, npcPlane);
      if (players.contains(south)) return false;
      WorldPosition west = new WorldPosition(npcX, npcY - 1, npcPlane);
      return !players.contains(west);
    };
  }

  public Polygon getNpcCube(NpcType npcType, WorldPosition npcPosition) {
    SceneManager sceneManager = script.getSceneManager();
    double localX = npcPosition.getPreciseX() - sceneManager.getSceneBaseTileX();
    double localY = npcPosition.getPreciseY() - sceneManager.getSceneBaseTileY();
    if (localX > 128 && localY > 128) return null;

    Polygon npcCube = script.getSceneProjector().getTileCube(
      localX,
      localY,
      npcPosition.getPlane(),
      npcType.bottomHeight,
      npcType.height);
    if (npcCube == null) return null;

    return npcCube.getResized(npcType.resizeFactor);
  }

  public boolean drawNpc(NpcType npcType, WorldPosition npcPosition, Color color) {
    Polygon npcCube = getNpcCube(npcType, npcPosition);
    if (npcCube == null) return false;

    String key = getNpcDrawableKey(npcType, npcPosition);
    script.getScreen().queueCanvasDrawable(
      key,
      canvas -> {
        canvas.fillPolygon(npcCube, color.getRGB(), 0.3);
        canvas.drawPolygon(npcCube, color.getRGB(), 0.6);
      });
    return true;
  }

  public void removeNpcDrawable(NpcType npcType, WorldPosition npcPosition) {
    String key = getNpcDrawableKey(npcType, npcPosition);
    script.getScreen().removeCanvasDrawable(key);
  }

  private String getNpcDrawableKey(NpcType npcType, WorldPosition npcPosition) {
    return npcType.name + "=" + npcPosition;
  }
}

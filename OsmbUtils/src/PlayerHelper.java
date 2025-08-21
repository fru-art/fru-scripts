import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.SceneManager;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;

public class PlayerHelper {
  private final Script script;

  public PlayerHelper(Script script) {
    this.script = script;
  }

  public Polygon getPlayerCube() {
    SceneManager sceneManager = script.getSceneManager();
    WorldPosition position = script.getWorldPosition();
    double localX = position.getPreciseX() - sceneManager.getSceneBaseTileX();
    double localY = position.getPreciseY() - sceneManager.getSceneBaseTileY();
    if (localX > 128 && localY > 128) return null;

    Polygon npcCube = script.getSceneProjector().getTileCube(
      localX,
      localY,
      position.getPlane(),
      0,
      225
      );
    if (npcCube == null) return null;

    return npcCube.getResized(0.8);
  }

  public boolean isPlayerAnimating() {
    return script.getPixelAnalyzer().isPlayerAnimating(0.25);
  }
}

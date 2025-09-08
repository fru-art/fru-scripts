import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.minimap.orbs.PrayerOrb;
import com.osmb.api.ui.minimap.MinimapOrbs;
import com.osmb.api.ui.overlay.HealthOverlay;
import com.osmb.api.ui.tabs.Inventory;
import task.Task;

import java.util.Comparator;
import java.util.List;

public class KillPestsTask extends Task {
  private static final WorldPosition KNIGHT = new WorldPosition(2656, 2592, 0);
  private final PestControlScript script;

  public KillPestsTask(PestControlScript script) {
    super(script);
    this.script = script;
  }

  @Override
  public boolean canExecute() {
    return script.getWorldPosition().getRegionID() == PestControlScript.MINIGAME_REGION &&
      script.getWidgetManager().getMinimap() != null;
  }

  @Override
  public boolean execute() {
    Inventory inventory = script.getWidgetManager().getInventory();
    if (inventory != null && !inventory.isOpen()) {
      inventory.open();
    }

    MinimapOrbs minimapOrbs = script.getWidgetManager().getMinimapOrbs();
    if (minimapOrbs != null && !minimapOrbs.isQuickPrayersActivated()) {
      minimapOrbs.setQuickPrayers(true);
    }

    List<WorldPosition> pests = script.getWidgetManager().getMinimap().getNPCPositions().asList().stream()
      .filter(pest -> pest.distanceTo(KNIGHT) > 0.1)
      .filter(pest -> getPestPolygon(pest) != null)
      .sorted(Comparator.comparing(pest -> PestControlScript.NEAR_KNIGHT.contains(pest) ? 0 : 1))
      .sorted(Comparator.comparingDouble(pest -> pest.distanceTo(script.getWorldPosition())))
      .toList();

    Double attackedInitialDistance = null;
    for (WorldPosition pest : pests) {
      Polygon polygon = getPestPolygon(pest);
      if (polygon == null) continue;
      try {
        if (script.getFinger().tapGameScreen(polygon, "Attack")) {
          attackedInitialDistance = pest.distanceTo(script.getWorldPosition());
          break;
        }
      } catch (Exception ignored) {}
    }

    if (attackedInitialDistance == null) {
      script.log(getClass(), "Failed to attack a pest");
      return false;
    }

    if (script.pollFramesHuman(
      () -> {
        Integer pestHitpoints = getHealthOverlayHitpoints();
        return pestHitpoints != null && pestHitpoints > 0;
      },
      (int) (attackedInitialDistance * 1_200 + 600))) {
      script.log(getClass(), "Failed find pest hitpoints");
      return false;
    }


    return script.pollFramesHuman(() -> {
      Integer pestHitpoints = getHealthOverlayHitpoints();
      return pestHitpoints == null || pestHitpoints == 0;
    }, Integer.MAX_VALUE);
  }

  private Polygon getPestPolygon(WorldPosition pest) {
    Polygon polygon = script.getSceneProjector().getTileCube(pest, 100);
    if (polygon == null) return null;
    return polygon.getResized(0.6);
  }

  private Integer getHealthOverlayHitpoints() {
    HealthOverlay healthOverlay = new HealthOverlay(script);
    if (!healthOverlay.isVisible()) return null;
    HealthOverlay.HealthResult healthResult = (HealthOverlay.HealthResult) healthOverlay.getValue(HealthOverlay.HEALTH);
    if (healthResult == null) return null;
    return healthResult.getCurrentHitpoints();
  }
}

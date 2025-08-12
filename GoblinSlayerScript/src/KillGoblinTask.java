import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.overlay.HealthOverlay;
import com.osmb.api.walker.pathing.CollisionManager;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillGoblinTask extends Task {
  private final DrawHelper drawHelper;
  private final EntityHelper entityHelper;
  private final WaitHelper waitHelper;

  private final Random random;

  public KillGoblinTask(Script script) {
    super(script);

    drawHelper = new DrawHelper(script);
    entityHelper = new EntityHelper(script);
    waitHelper = new WaitHelper(script);

    random = new Random();
  }

  @Override
  public boolean canExecute() {
    return script.getWidgetManager().getMinimapOrbs().getHitpoints().get() > 6;
  }

  @Override
  public boolean execute() {
    // Check for idle before grabbing positions of NPCs
    if (!waitHelper.waitForNoChange("Idle", entityHelper::isPlayerIdling, 2_000, 10_000)) {
      script.log(getClass(), "Failed to stop moving");
      return false;
    }

    WorldPosition position = script.getWorldPosition();
    List<WorldPosition> npcs = script.getWidgetManager().getMinimap().getNPCPositions().asList();
    List<WorldPosition> players = script.getWidgetManager().getMinimap().getPlayerPositions().asList();

    List<WorldPosition> unoccupiedNpcs = npcs.stream()
      .filter(npc -> {
        for (WorldPosition player : players) {
          if (CollisionManager.isCardinallyAdjacent(npc, player)) {
            drawHelper.drawPosition(npc, Color.RED);
            return false;
          }
        }
        drawHelper.drawPosition(npc, Color.GREEN);
        return true;
      })
      .sorted(Comparator.comparingInt((WorldPosition npc) -> npc.distanceTo(position)))
      .limit(random.nextInt(4)) // Check up to 3 positions where unoccupied NPCs just were
      .toList();

    if (unoccupiedNpcs.isEmpty()) {
      script.log(getClass(), "Failed to find unoccupied goblins");
      return false;
    }

    WorldPosition attackedNpc = null;
    for (WorldPosition unoccupiedNpc : unoccupiedNpcs) {
      Shape tilePoly = script.getSceneProjector().getTilePoly(unoccupiedNpc);
      if (script.getFinger().tapGameScreen(tilePoly, menuEntries -> menuEntries.stream()
        .filter(item -> "attack goblin".equalsIgnoreCase(item.getRawText()))
        .findFirst()
        .orElse(null))) {
        attackedNpc = unoccupiedNpc;
        break;
      }
    }

    if (attackedNpc == null) {
      script.log(getClass(), "Failed to attack goblin");
      return false;
    }

    HealthOverlay healthOverlay = new HealthOverlay(script);
    if (!script.submitHumanTask(healthOverlay::isVisible, position.distanceTo(attackedNpc) * 1_000)) {
      script.log(getClass(), "Failed to instantiate health overlay");
      return false;
    }

    boolean killed = waitHelper.waitForNoChange(
      "Goblin health",
      () -> ((HealthOverlay.HealthResult) healthOverlay.getValue(HealthOverlay.HEALTH)).getCurrentHitpoints(),
      8_000, // Should not take more than 8s to damage goblin
      16_000, // Should not take more than 16s to kill goblin
      () -> {
        int health = ((HealthOverlay.HealthResult) healthOverlay.getValue(HealthOverlay.HEALTH)).getCurrentHitpoints();
        return !healthOverlay.isVisible() || health == 0;
      });

    if (!killed) {
      script.log(getClass(), "Failed to kill goblin");
      return false;
    }

    return true;
  }
}

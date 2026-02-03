import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.minimap.MinimapOrbs;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.world.World;
import helper.*;
import task.Task;
import task.TaskScript;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PickpocketTask extends Task {
  private final List<RectangleArea> ignoreAreas;
  private final WorldPosition nearPosition;
  private final NpcType npcType;

  private final DetectionHelper detectionHelper;
  private final DrawHelper drawHelper;
  private final InventoryHelper inventoryHelper;
  private final NpcHelper npcHelper;

  private final Random random = new Random();

  private boolean canSingleTap = false;
  private int coinPouchCheck = random.nextInt(20, 25);

  public PickpocketTask(TaskScript script, NpcType npcType, WorldPosition nearPosition, List<RectangleArea> ignoreAreas) {
    super(script);
    retryLimit = 5;

    this.ignoreAreas = ignoreAreas;
    this.nearPosition = nearPosition;
    this.npcType = npcType;

    detectionHelper = new DetectionHelper(script);
    drawHelper = new DrawHelper(script);
    inventoryHelper = new InventoryHelper(script, Set.of(ItemID.COIN_POUCH));
    npcHelper = new NpcHelper(script);
  }

  public PickpocketTask(TaskScript script, NpcType npcType, WorldPosition nearPosition) { // , List<RectangleArea> ignoreAreas
    this(script, npcType, nearPosition, List.of());
  }

  public boolean canExecute(boolean skipInventoryWait) {
    Integer hitpoints = getHitpoints();
    if (hitpoints == null || hitpoints < npcType.minimumHitpointsToInteract) {
      script.log(getClass(), "Failed to get hitpoint from minimap orbs");
      return false;
    }

    ItemGroupResult snapshot = inventoryHelper.getSnapshot(skipInventoryWait);
    if (snapshot == null) {
      if (skipInventoryWait) script.log(getClass(), "Failed to get snapshot");
      return skipInventoryWait;
    }
    // TODO: Incorrect if inventory can hold more coin pouches
    if (snapshot.isFull()) {
      script.log(getClass(), "Skipping pickpocketing due to inventory full");
      return false;
    }
    if (snapshot.getAmount(ItemID.COIN_POUCH) >= coinPouchCheck) {
      coinPouchCheck = random.nextInt(20, 28);
      script.log(getClass(), "Skipping pickpocketing due to coin pouches");
      return false;
    }

    return true;
  }

  @Override
  public boolean canExecute() {
    return canExecute(false);
  }

  @Override
  public boolean execute() {
    AtomicReference<List<WorldPosition>> npcs = new AtomicReference<>(getHighlightedNpcs());

    // Walk to near position
    if (npcs.get().isEmpty()) {
      WalkConfig walkConfig = new WalkConfig.Builder()
        .breakCondition(() -> {
          npcs.set(getHighlightedNpcs());
          return !npcs.get().isEmpty();
        })
        .breakDistance(1)
        .build();
      script.getWalker().walkTo(nearPosition, walkConfig);
    }

    // Wait a few seconds for NPCs
    if (npcs.get().isEmpty()) {
      script.pollFramesUntil(() -> {
          npcs.set(getHighlightedNpcs());
          return !npcs.get().isEmpty();
        }, 3_000);
    }

    // If no NPCs are available, hop worlds and retry as fail
    if (npcs.get().isEmpty()) {
      script.getProfileManager().forceHop();
      script.pollFramesHuman(() -> script.getWidgetManager().getGameState() == GameState.LOGGED_IN, 30_000);
      return false;
    }

    AtomicReference<WorldPosition> lastNpc = new AtomicReference<>(null);
    AtomicLong lastNpcTime = new AtomicLong();

    while (canExecute(true)) {
      boolean waitForNpc = script.pollFramesUntil(() -> {
        npcs.set(getHighlightedNpcs());
        if (npcs.get() == null || npcs.get().isEmpty()) return false;
        WorldPosition npc = npcs.get().get(0);

        // Best-effort heuristic based on distance
        boolean isNpcSameAsLast = lastNpc.get() == null ||
          lastNpc.get().distanceTo(npc) < (double) (System.currentTimeMillis() - lastNpcTime.get()) / 600 + 1;

        return !npcs.get().isEmpty() && isNpcSameAsLast;
      }, 3_000);
      if (!waitForNpc) return true;

      WorldPosition npc = npcs.get().get(0);
      assert npc != null;

      Polygon cube = npcHelper.getNpcCube(npcType, npc);
      if (cube == null) return false;

      Integer hitpoints = getHitpoints();
      int initialHitpoints = hitpoints == null ? Integer.MAX_VALUE : hitpoints;

      try {
        boolean result;

        if (canSingleTap) {
          result = script.getFinger().tapGameScreen(cube);
        } else {
          result = script.getFinger().tapGameScreen(cube, (entries) -> {
            drawHighlightedNpcs();
            if (!canExecute(true)) return null;

            for (int i = 0; i < entries.size(); i++) {
              MenuEntry entry = entries.get(i);
              if (entry.getAction().startsWith("pickpocket")) {
                if (i == 0) canSingleTap = true;
                return entry;
              }
            }
            return null;
          });
        }

        if (!result) return false;
      } catch (Exception e) {
        return false;
      }

      lastNpc.set(npc);
      lastNpcTime.set(System.currentTimeMillis());

      // Got hit and stunned
      hitpoints = getHitpoints();
      if (hitpoints != null && hitpoints < initialHitpoints) {
        // Wait for NPC chat to go away before using NPC query again; NPC chat moves around the label
        script.pollFramesUntil(() -> {
          if (!canExecute(true)) return true; // Exit early, loop will exit anyway e.g. need to eat
          drawHighlightedNpcs();
          return false;
        }, random.nextInt(2_500, 3_500));
      }

      // Try not to get stuck in rooms
      if (inIgnoreArea(script.getWorldPosition())) {
        WalkConfig walkConfig = new WalkConfig.Builder()
          .breakCondition(() -> !inIgnoreArea(script.getWorldPosition()))
          .build();
        // Stuck in building
        if (!script.getWalker().walkTo(nearPosition, walkConfig) || inIgnoreArea(script.getWorldPosition())) {
          script.stop();
          script.log(getClass(), "Failed to leave restricted area");
          return false;
        }
      }
    }

    return true;
  }

//  private int getRandomWait() {
//    int randomInt = random.nextInt(1000);
//
////    if (randomInt < 25) {
////      return random.nextInt(700, 1_400);
////    } else if (randomInt < 100) {
////      return random.nextInt(350, 450);
////    }
////    else
//    if (randomInt < 400) {
//      return random.nextInt(100, 200);
//    } else {
//      return random.nextInt(50, 150);
//    }
//  }

  private void drawHighlightedNpcs() {
    getHighlightedNpcs();
  }

  private List<WorldPosition> getHighlightedNpcs() {
    return npcHelper.getHighlightedNpcs(npcType, npc -> !inIgnoreArea(npc));
  }

  private boolean inIgnoreArea(WorldPosition position) {
    for (RectangleArea ignoreArea : ignoreAreas) {
      if (ignoreArea.contains(position)) return true;
    }
    return false;
  }

  private Integer getHitpoints() {
    MinimapOrbs minimapOrbs = script.getWidgetManager().getMinimapOrbs();
    if (minimapOrbs == null) return null;
    return minimapOrbs.getHitpoints();
  }
}

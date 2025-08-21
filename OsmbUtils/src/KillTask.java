import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.minimap.orbs.PrayerOrb;
import com.osmb.api.ui.minimap.MinimapOrbs;
import com.osmb.api.ui.overlay.HealthOverlay;
import com.osmb.api.utils.UIResult;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class KillTask extends Task {
  public static final Map<Integer, Integer> FOOD_TO_HEALTH = Map.ofEntries(
    Map.entry(ItemID.ANGLERFISH, 22),
    Map.entry(ItemID.BASS, 13),
    Map.entry(ItemID.BREAD, 5),
    Map.entry(ItemID.COOKED_CHICKEN, 3),
    Map.entry(ItemID.COOKED_KARAMBWAN, 18),
    Map.entry(ItemID.COOKED_MEAT, 3),
    Map.entry(ItemID.CURRY, 19),
    Map.entry(ItemID.DARK_CRAB, 22),
    Map.entry(ItemID.HERRING, 5),
    Map.entry(ItemID.IXCOZTIC_WHITE, 16),
    Map.entry(ItemID.JUG_OF_WINE, 11),
    Map.entry(ItemID.LOBSTER, 12),
    Map.entry(ItemID.MACKEREL, 6),
    Map.entry(ItemID.MANTA_RAY, 22),
    Map.entry(ItemID.MONKFISH, 16),
    Map.entry(ItemID.PEACH, 8),
    Map.entry(ItemID.PIKE, 8),
    Map.entry(ItemID.POTATO_WITH_CHEESE, 16),
    Map.entry(ItemID.SALMON, 9),
    Map.entry(ItemID.SEA_TURTLE, 21),
    Map.entry(ItemID.SHARK, 20),
    Map.entry(ItemID.SHRIMPS, 3),
    Map.entry(ItemID.SNOWY_KNIGHT, 15),
    Map.entry(ItemID.SWORDFISH, 14),
    Map.entry(ItemID.TROUT, 7),
    Map.entry(ItemID.TUNA, 10),
    Map.entry(ItemID.TUNA_POTATO, 22)
  );
  public static final Set<Integer> food = FOOD_TO_HEALTH.keySet();
  private static final int HITSPLAT = 1359;
  private static final int MAXSPLAT = 3571;
  private static final int OTHER_HITSPLAT = 1631;
  private static final int OTHER_WHIFFSPLAT = 1630;
  private static final int WHIFFSPLAT = 1358;

  private final Set<Integer> lootables;
  private final Set<String> lootablesNames;
  private final NpcType npcType;
  private final boolean shouldPrayerFlick;

  private final List<SearchableImage> otherPlayerSplats;
  private final List<SearchableImage> playerSplats;

  private WorldPosition lastAttackedNpcPosition;

  private final DrawHelper drawHelper;
  private final InventoryHelper inventoryHelper;
  private final NpcHelper npcHelper;
  private final PlayerHelper playerHelper;
  private final Random random;
  private final TickCounter tickCounter;
  private final WaitHelper waitHelper;

  /**
   * @param script
   * @param npcType
   * @param lootables
   * @param shouldPrayerFlick Do not use
   */
  @Deprecated
  public KillTask(Script script, NpcType npcType, Set<Integer> lootables, boolean shouldPrayerFlick) {
    super(script);
    this.lootables = lootables;
    lootablesNames = lootables.stream()
      .map(itemId -> script.getItemManager().getNameForItemID(itemId).toLowerCase())
      .collect(Collectors.toSet());
    this.npcType = npcType;
    this.shouldPrayerFlick = shouldPrayerFlick;

    otherPlayerSplats = Stream.of(
      getSplatCutoutQuarters(OTHER_HITSPLAT),
      getSplatCutoutQuarters(OTHER_WHIFFSPLAT)
    ).flatMap(List::stream).collect(Collectors.toList());
    playerSplats = Stream.of(
      getSplatCutoutQuarters(HITSPLAT),
      getSplatCutoutQuarters(MAXSPLAT),
      getSplatCutoutQuarters(WHIFFSPLAT)
    ).flatMap(List::stream).collect(Collectors.toList());

    drawHelper = new DrawHelper(script);
    inventoryHelper = new InventoryHelper(script, Stream.concat(
      lootables.stream(),
      food.stream()
    ).collect(Collectors.toSet()));
    npcHelper = new NpcHelper(script);
    playerHelper = new PlayerHelper(script);
    random = new Random();
    tickCounter = new TickCounter();
    waitHelper = new WaitHelper(script);
  }

  public KillTask(Script script, NpcType npcType, Set<Integer> lootables) {
    this(script, npcType, lootables, false);
  }

  @Override
  public boolean canExecute() {
    while(maybeEat()) {} // Maybe eat and also open inventory
    return script.getWidgetManager().getMinimapOrbs().getHitpoints().get() >= npcType.minimumHitpointsToFight;
  }

  @Override
  public boolean execute() {
    Set<WorldPosition> ignoreablePositions = new HashSet<>();

    Integer npcHitpoints = getHealthOverlayHitpoints();
    // If NPC HP is 0, ignore recently killed NPC
    if (npcHitpoints != null && npcHitpoints == 0 && lastAttackedNpcPosition != null) {
      ignoreablePositions.add(lastAttackedNpcPosition);
      lastAttackedNpcPosition = null;
    }
    // Find and attack an NPC if no NPC HP available (not fighting) or NPC HP is 0 (recently killed NPC)
    if (npcHitpoints == null || npcHitpoints == 0) {
      if (!attackNpc(ignoreablePositions)) {
        return false;
      }
    }

    assert lastAttackedNpcPosition != null;

    if (!script.submitTask(() -> {
      if (getHealthOverlayHitpoints() == null) return false;
      tickCounter.sync();
      return true;
    }, script.getWorldPosition().distanceTo(lastAttackedNpcPosition) * 1_000,
      false, true)) {
      script.log(getClass(), "Failed to begin fighting a " + npcType.name);
      return false;
    }

    AtomicReference<Integer> atomicNpcHitpoints = new AtomicReference<>(getHealthOverlayHitpoints());
    AtomicReference<Long> atomicTickId = new AtomicReference<>(tickCounter.getCurrentTickId());
    if (shouldPrayerFlick && script.getWidgetManager().getMinimapOrbs().getPrayerPoints().get() > 0) {
      script.getWidgetManager().getMinimapOrbs().setQuickPrayers(true);
    }

    while(atomicNpcHitpoints.get() != null && atomicNpcHitpoints.get() > 0) {
      // Sanity check that damage is being dealt to the NPC or that hitsplats are showing. If no action occurs within
      // the timeout duration, then fail.
      boolean isDamageBeingDealt = script.submitTask(() -> {
        maybeEat();
        maybeFlickPrayer(atomicTickId);

        boolean returnIsDamageBeingDealt = false;

        Integer nextNpcHitpoints = getHealthOverlayHitpoints();
        if (nextNpcHitpoints != null) {
          if (!nextNpcHitpoints.equals(atomicNpcHitpoints.get())) {
            script.log(getClass(), "Hit " + npcType.name + " for " + (atomicNpcHitpoints.get() - nextNpcHitpoints));
            tickCounter.sync(); // Don't sync on splats because those detectors run multiple times per splat instance
            returnIsDamageBeingDealt = true;
          }

          atomicNpcHitpoints.set(nextNpcHitpoints);
        }

        // Find hitsplat, which draws, and save npc position and last hitsplat time
        WorldPosition npcWithSplat = getNpcWithSplat();
        if (npcWithSplat != null) {
          lastAttackedNpcPosition = npcWithSplat;
          returnIsDamageBeingDealt = true;
        }

        if(getPlayerWithSplat()) {
          returnIsDamageBeingDealt = true;
        }

        return returnIsDamageBeingDealt;
      }, 5_000, false, true);

      if (!isDamageBeingDealt) {
        script.log(getClass(), "Failed to finish fighting");
        return false;
      }
    }

    definitelyDisableQuickPrayer();

    if (npcHitpoints == null) {
      script.log(getClass(), "Killed " + npcType.name + " by way of hitpoints no longer available");
    } else if (npcHitpoints == 0) {
      script.log(getClass(), "Killed " + npcType.name + " by reducing hitpoints to 0");
    }

    if (lootables == null || lootables.isEmpty()) return true;
    // Wait 3s for loot to appear

    script.submitHumanTask(() -> {
      Set<WorldPosition> itemPositions = new HashSet<>(
        script.getWidgetManager().getMinimap().getItemPositions().asList());

      return itemPositions.contains(lastAttackedNpcPosition);
    }, 3_000, false, true);

    loot(lastAttackedNpcPosition);
    return true;
  }

  private void maybeFlickPrayer(AtomicReference<Long> atomicLastTickId) {
    if (!shouldPrayerFlick) return;
    if (script.getWidgetManager().getMinimapOrbs().getPrayerPoints().get() <= 0) return;
    if (atomicLastTickId.get() == tickCounter.getCurrentTickId()) return;

    PrayerOrb prayerOrb = (PrayerOrb) script.getWidgetManager().getComponent(PrayerOrb.class);
    Rectangle tappableBounds = prayerOrb.getTappableBounds().get().getResized(0.25);
    if (tappableBounds == null) {
      script.log(getClass(), "Failed to find prayer orb");
      return;
    }

    script.log(getClass(), "Flicking prayer"); // TODO: Remove

    script.getFinger().tap(false, tappableBounds);
    script.submitTask(() -> true, random.nextInt(100, 150));
    script.getFinger().tap(false, tappableBounds);
    atomicLastTickId.set(tickCounter.getCurrentTickId());
  }

  private boolean maybeEat() {
    MinimapOrbs minimapOrbs = script.getWidgetManager().getMinimapOrbs();
    UIResult<Integer> hitpointsResult = minimapOrbs.getHitpoints();
    UIResult<Integer> hitpointsPercentageResult = minimapOrbs.getHitpointsPercentage();
    if (hitpointsResult == null || hitpointsPercentageResult == null) return false;

    Integer hitpoints = hitpointsResult.get();
    Integer hitpointsPercentage = hitpointsPercentageResult.get();
    if (hitpoints == null || hitpointsPercentage == null) return false;

    ItemSearchResult item = inventoryHelper.getSnapshot(true).getItem(food);
    if (item == null) return false;

    int itemId = item.getId();
    String itemName = script.getItemManager().getNameForItemID(itemId).toLowerCase();

    // TODO: Make more sophisticated
    int eatHitpoints = npcType.minimumHitpointsToFight;
    if (hitpoints > eatHitpoints) return false;

    script.log(getClass(), "Eating " + itemName + " because hitpoints dropped below " + eatHitpoints);
    script.submitHumanTask(() -> true, 0, false, true); // Human delay before eating
    item.interact();
    return script.submitHumanTask(() -> !Objects.equals(minimapOrbs.getHitpoints().get(), hitpoints), 2_000);
  }

  private boolean isLootOption(MenuEntry menuEntry) {
    String menuEntryText = menuEntry.getRawText();
    if (!menuEntryText.startsWith("take ")) return false;

    String item = menuEntryText.substring("take ".length());
    return lootablesNames.contains(item);
  }

  private void loot(WorldPosition position) {
    AtomicInteger lastEntriesSize = new AtomicInteger(-1);
    AtomicInteger lastLootedIndex = new AtomicInteger(0);
    AtomicBoolean maybeHasLoot =  new AtomicBoolean(true);

    script.log(getClass(), "Searching through item pile for " + lootablesNames);
    while(maybeHasLoot.get()) {
      Polygon lootCube = script.getSceneProjector()
        .getTileCube(position, 10).getResized(0.5);
      int initialLootAmountInInventory = inventoryHelper.getSnapshot().getAmount(lootables);

      boolean didTake = script.getFinger().tapGameScreen(lootCube, menuEntries -> {
        int startIndex = lastEntriesSize.get() == menuEntries.size() ?
          lastLootedIndex.get() + 1:
          lastLootedIndex.get();
        if (startIndex >= menuEntries.size()) {
          script.log(getClass(), "No remaining options available");
          return null;
        }

        int[] lootIndices = IntStream.range(startIndex, menuEntries.size())
          .filter(i -> isLootOption(menuEntries.get(i)))
          .toArray();
        int firstLootOptionIndex = Arrays.stream(lootIndices).findFirst().orElse(-1);

        if (lootIndices.length == 0 || firstLootOptionIndex == -1) {
          script.log(getClass(), "Remaining options did not include loot");
          return null;
        }

        if (lootIndices.length == 1) maybeHasLoot.set(false);

        MenuEntry lootOption = menuEntries.get(firstLootOptionIndex);
        script.log(getClass(), "Taking " + lootOption.getEntityName());
        lastLootedIndex.set(firstLootOptionIndex);
        lastEntriesSize.set(menuEntries.size());
        return lootOption;
      });

      if (didTake) {
        script.submitHumanTask(
          () -> inventoryHelper.getSnapshot().getAmount(lootables) > initialLootAmountInInventory,
          Math.max(script.getWorldPosition().distanceTo(position) * 1_000, 500),
          false, true);
      } else {
        break;
      }
    }
  }

  private boolean attackNpc(Set<WorldPosition> ignoreablePositions) {
    // Check for idle before grabbing positions of NPCs
    if (!waitHelper.waitForNoChange(
      "Position",
      script::getWorldPosition,
      300,
      3_000)) {
      script.log(getClass(), "Failed to stop moving");
      return false;
    }

    List<WorldPosition> attackableNpcs = getAttackableNpcs(ignoreablePositions)
      .stream().limit(random.nextInt(3, 5)).toList();

    WorldPosition attackedNpc = null;
    for (WorldPosition attackableNpc : attackableNpcs) {
      List<WorldPosition> updatedNpcs = script.getWidgetManager().getMinimap().getNPCPositions().asList();
      if (updatedNpcs == null || !updatedNpcs.contains(attackableNpc)) {
        npcHelper.removeNpcDrawable(npcType, attackableNpc);
        npcHelper.drawNpc(npcType, attackableNpc, Color.GRAY);
        continue;
      }

      Polygon npcCube = npcHelper.getNpcCube(npcType, attackableNpc);
      if (script.getFinger().tapGameScreen(npcCube, menuEntries -> menuEntries.stream()
        .filter(item -> ("attack " + npcType.name).equalsIgnoreCase(item.getRawText()))
        .findFirst()
        .orElse(null))) {
        attackedNpc = attackableNpc;
        break;
      }
    }

    if (attackedNpc == null) {
      script.log(getClass(), "Failed to attack a " + npcType.name);
      return false;
    }

    npcHelper.removeNpcDrawable(npcType, attackedNpc);
    npcHelper.drawNpc(npcType, attackedNpc, Color.BLUE);
    lastAttackedNpcPosition = attackedNpc;

    return true;
  }

  private void definitelyDisableQuickPrayer() {
    if (script.submitTask(
      () -> script.getWidgetManager().getMinimapOrbs().isQuickPrayersActivated().get(),
      2_000, false, true
    )) {
      script.getWidgetManager().getMinimapOrbs().setQuickPrayers(false);
    }
  }

  /**
   * @return a list of NPCs available to attack. They are not guaranteed to be the correctly named NPC until the menu
   * options are validated.
   */
  private List<WorldPosition> getAttackableNpcs(Set<WorldPosition> ignoreablePositions) {
    NpcMatcher unoccupiedNpcMatcher = npcHelper.getUnoccupiedNpcMatcher();
    WorldPosition position = script.getWorldPosition();

    List<WorldPosition> attackableNpcs = script.getWidgetManager().getMinimap().getNPCPositions().asList().stream()
      .filter(npc -> !ignoreablePositions.contains(npc))
      // Best-effort heuristic for ignoring moving targets as they are difficult to loop through and check if they are
      // the correct NPC type
      .filter(npc -> {
        if (!npcHelper.getNpcStill(npc)) {
          npcHelper.drawNpc(npcType, npc, Color.YELLOW);
          return false;
        }
        return true;
      })
      // Best-effort heuristic for ignoring targets being attacked by other players using melee. These adjacent players
      // could potentially not be attacking the NPC.
      .filter(npc -> {
        if (!unoccupiedNpcMatcher.test(npc)) {
          npcHelper.drawNpc(npcType, npc, Color.ORANGE);
          return false;
        }
        return true;
      })
      // Best-effort heuristic for ignoring targets with active splats from other players. The NPC could be fighting and
      // be between attacks by other players, displaying no splats.
      .filter(npc -> {
        Polygon npcCube = npcHelper.getNpcCube(npcType, npc);
        if (npcCube == null) return false;
        Rectangle splatBounds = getSplatBounds(npcCube);

        ImageSearchResult result = findSplat(npcCube, otherPlayerSplats);
        if (result != null) {
          script.getScreen().getDrawableCanvas().drawRect(splatBounds, Color.RED.getRGB(), 0.6);
          script.getScreen().getDrawableCanvas().fillRect(splatBounds, Color.RED.getRGB(), 0.15);
          script.getScreen().getDrawableCanvas().drawRect(result.getBounds(), Color.RED.getRGB(), 0.45);
          script.getScreen().getDrawableCanvas().fillRect(result.getBounds(), Color.RED.getRGB(), 0.15);
          return false;
        }

        return true;
      })
      .sorted(Comparator.comparingInt(npc -> npc.distanceTo(position)))
      .toList();

    for (WorldPosition attackableNpc : attackableNpcs) {
      npcHelper.drawNpc(npcType, attackableNpc, Color.GREEN);
    }

    return attackableNpcs;
  }

  private boolean getPlayerWithSplat() {
    Polygon playerCube = playerHelper.getPlayerCube();
    ImageSearchResult result = findSplat(playerHelper.getPlayerCube(), playerSplats);
    if (result == null) return false;

    Rectangle splatBounds = getSplatBounds(playerCube);
    script.getScreen().queueCanvasDrawable("attackedPlayer", canvas -> {
      script.getScreen().getDrawableCanvas().drawRect(splatBounds, Color.MAGENTA.getRGB(), 0.6);
      script.getScreen().getDrawableCanvas().fillRect(splatBounds, Color.MAGENTA.getRGB(), 0.15);
      script.getScreen().getDrawableCanvas().drawRect(result.getBounds(), Color.MAGENTA.getRGB(), 0.45);
      script.getScreen().getDrawableCanvas().fillRect(result.getBounds(), Color.MAGENTA.getRGB(), 0.15);
    });

    return true;
  }

  private WorldPosition getNpcWithSplat() {
    if (getHealthOverlayHitpoints() == null) return null;

    return script.getWidgetManager().getMinimap().getNPCPositions().asList().stream()
      .filter(npc -> {
        Polygon npcCube = npcHelper.getNpcCube(npcType, npc);
        if (npcCube == null) return false;

        ImageSearchResult result = findSplat(npcCube, playerSplats);
        if (result == null) return false;

        Rectangle splatBounds = getSplatBounds(npcCube);
        script.getScreen().queueCanvasDrawable("attackedNpc", canvas -> {
          script.getScreen().getDrawableCanvas().drawRect(splatBounds, Color.CYAN.getRGB(), 0.6);
          script.getScreen().getDrawableCanvas().fillRect(splatBounds, Color.CYAN.getRGB(), 0.15);
          script.getScreen().getDrawableCanvas().drawRect(result.getBounds(), Color.CYAN.getRGB(), 0.45);
          script.getScreen().getDrawableCanvas().fillRect(result.getBounds(), Color.CYAN.getRGB(), 0.15);
        });

        return true;
      }).findFirst().orElse(null);
  }

  private Rectangle getSplatBounds(Polygon npcCube) {
    Rectangle npcBounds = npcCube.getBounds();
    // Add padding for NPC bounds smaller than hit-splat dimensions
    int padding = Math.max(Math.max((24 - npcBounds.width) / 2, (24 - npcBounds.height) / 2) , 0);
    Rectangle screenBounds = script.getScreen().getBounds();

    int leftBound = Math.max(screenBounds.x, npcBounds.x - padding);
    int rightBound = Math.min(screenBounds.x + screenBounds.width,  npcBounds.x + npcBounds.width + padding);
    int topBound = Math.max(screenBounds.y, npcBounds.y - padding);
    int bottomBound =
      Math.min(screenBounds.y + screenBounds.getHeight(), npcBounds.y + npcBounds.getHeight() + padding);

    return new Rectangle(
      leftBound,
      topBound,
      rightBound - leftBound,
      bottomBound - topBound);
  }

  private ImageSearchResult findSplat(Polygon cube, List<SearchableImage> splats) {
    Rectangle splatBounds = getSplatBounds(cube);
    return this.script.getImageAnalyzer().findLocation(splatBounds, splats.toArray(new SearchableImage[0]));
  }

  private List<SearchableImage> getSplatCutoutQuarters(int spriteId) {
    Canvas canvas = new Canvas(spriteId, script);
    canvas.fillRect(
      3, 3, canvas.canvasWidth - 6, canvas.canvasHeight - 6, ColorUtils.TRANSPARENT_PIXEL);

    Image canvasImage = canvas.toImage();
    int halfWidth = canvasImage.width / 2;
    int halfHeight = canvasImage.height / 2;

    List<Image> canvasQuarterImages = List.of(
      canvasImage.subImage(0, 0, halfWidth, halfHeight),
      canvasImage.subImage(0, halfHeight, halfWidth, canvasImage.height - halfHeight),
      canvasImage.subImage(halfWidth, 0, canvasImage.width - halfWidth, halfHeight),
      canvasImage.subImage(
        halfWidth, halfHeight, canvasImage.width - halfWidth, canvasImage.height - halfHeight)
    );

    return canvasQuarterImages.stream()
      .map(image -> image.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)).toList();
  }

  private Integer getHealthOverlayHitpoints() {
    HealthOverlay healthOverlay = new HealthOverlay(script);
    if (!healthOverlay.isVisible()) return null;
    HealthOverlay.HealthResult healthResult = (HealthOverlay.HealthResult) healthOverlay.getValue(HealthOverlay.HEALTH);
    if (healthResult == null) return null;
    return healthResult.getCurrentHitpoints();
  }
}

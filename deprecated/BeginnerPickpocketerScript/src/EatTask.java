import com.osmb.api.item.ItemGroup;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.minimap.MinimapOrbs;
import helper.InventoryHelper;
import task.Task;
import task.TaskScript;

import java.util.Map;
import java.util.Set;

public class EatTask extends Task {
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

  private final InventoryHelper inventoryHelper;

  private Integer healthToEat;
  private Double percentHealthToEat;

  public EatTask(TaskScript script, double percentHealthToEat) {
    super(script);
    this.percentHealthToEat = percentHealthToEat;

    inventoryHelper = new InventoryHelper(script, food);
  }

  public EatTask(TaskScript script, int healthToEat) {
    super(script);
    this.healthToEat = healthToEat;

    inventoryHelper = new InventoryHelper(script, food);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    if (snapshot == null || !snapshot.containsAny(food)) return false;

    MinimapOrbs minimapOrbs = script.getWidgetManager().getMinimapOrbs();
    if (minimapOrbs == null) return false;
    if (healthToEat != null) return minimapOrbs.getHitpoints() <= healthToEat;
    if (percentHealthToEat != null) return minimapOrbs.getHitpointsPercentage() <= percentHealthToEat * 100;
    return false;
  }

  @Override
  public boolean execute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    if (snapshot == null) return false;

    MinimapOrbs minimapOrbs = script.getWidgetManager().getMinimapOrbs();
    int initialHealth = minimapOrbs.getHitpoints();
    snapshot.getItem(food).interact();
    return script.pollFramesHuman(() -> minimapOrbs.getHitpoints() != initialHealth, 1_800, true);
  }
}

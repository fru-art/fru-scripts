import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.bank.Bank;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BankWithdrawTask extends Task {
  // Sorted based on prevalence; insertion order matters
  private static final Map<String, String> BANK_TO_MENU_ITEM_MAP =
    new LinkedHashMap<>(Map.ofEntries(
      Map.entry("Bank booth", "Bank"),
      Map.entry("Bank chest", "Use"),
      Map.entry("Banker", "Bank")
    ));

  private final int desiredAmount;
  private final int withdrawThreshold;
  private final Set<Integer> withdrawables;
  private final Map<Integer, BankLocation> regionToBankMap;

  private final InventoryHelper inventoryHelper;
  private final WalkHelper walkHelper;

  public BankWithdrawTask(Script script,
                          Set<BankLocation> bankLocations,
                          Set<Integer> withdrawables,
                          int withdrawThreshold,
                          int desiredAmount) {
    super(script);

    this.desiredAmount = desiredAmount;
    this.withdrawThreshold = withdrawThreshold;
    this.withdrawables = withdrawables;
    regionToBankMap = BankLocation.getRegionToBankMap(bankLocations);

    inventoryHelper = new InventoryHelper(script, withdrawables);
    walkHelper = new WalkHelper(script);
  }
  public BankWithdrawTask(Script script,
                          Set<BankLocation> bankLocations,
                          Set<Integer> withdrawables) {
    this(script, bankLocations, withdrawables, 0, Integer.MAX_VALUE);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();
    if (inventorySnapshot.isFull()) {
      script.log(getClass(), "Skipping banking due to inventory full");
      return false;
    }

    Bank bank = script.getWidgetManager().getBank();
    if (bank.isVisible()) {
      return true;
    }

    Integer regionId = script.getWorldPosition().getRegionID();
    BankLocation bankLocation = regionToBankMap.get(regionId);
    if (bankLocation == null) {
      script.log(getClass(), "No bank location found for region " + regionId);
      return false;
    }

    for (String bankName : BANK_TO_MENU_ITEM_MAP.keySet()) {
      RSObject bankObject = script.getObjectManager().getClosestObject(bankName);
      if (bankObject != null && bankObject.isInteractableOnScreen() && bankObject.getTileDistance() < 5) {
        return true;
      }
    }

    if (inventorySnapshot.getAmount(withdrawables) > withdrawThreshold) {
      script.log(getClass(), "Skipping banking, found more than " + withdrawThreshold + " items:" + withdrawables);
      return false;
    }

    return true;
  }

  @Override
  public boolean execute() {
    Bank bank = script.getWidgetManager().getBank();

    // If interface aren't already open, try to walk and interact with some type of bank object
    if (!bank.isVisible()) {
      Integer regionId = script.getWorldPosition().getRegionID();
      BankLocation bankLocation = regionToBankMap.get(regionId);

      boolean interacted = false;
      for (String bankName : BANK_TO_MENU_ITEM_MAP.keySet()) {
        script.log(getClass(), "Checking bank " + bankName);
        String menuItem = BANK_TO_MENU_ITEM_MAP.get(bankName);
        WorldPosition approximateLocation = bankLocation.approximatePosition;
        interacted = walkHelper.walkToAndInteract(bankName, menuItem, approximateLocation);
        if (interacted) break;
      }
      if (!interacted) {
        script.log(getClass(), "Failed to walk and interact with any bank object");
        return false;
      }

      if (!script.submitHumanTask(() -> script.getWidgetManager().getBank().isVisible(), 3_000)) {
        script.log(getClass(), "Failed to open bank object interface");
        return false;
      }
    }

    boolean withdrewEnough = false;
    for (Integer itemId : withdrawables) {
      ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(withdrawables);
      if (!bankSnapshot.containsAny(itemId)) continue;

      int amountToWithdraw =  desiredAmount - inventoryHelper.getSnapshot().getAmount(withdrawables);
      script.getWidgetManager().getBank().withdraw(itemId, amountToWithdraw);

      ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();
      if (inventorySnapshot.getAmount(withdrawables) >= desiredAmount ||
        (inventorySnapshot.isFull() && inventorySnapshot.getAmount(withdrawables) > withdrawThreshold)) {
        withdrewEnough = true;
        break;
      }
    }

    if (!withdrewEnough) {
      script.log(getClass(), "Failed to withdraw enough items: " + withdrawables);
      return false;
    }

    return true;
  }
}

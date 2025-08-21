import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.ui.depositbox.DepositBox;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BankDepositTask extends Task {
  // Sorted based on prevalence; insertion order matters
  private static final Map<String, String> BANK_DEPOSIT_TO_MENU_ITEM_MAP =
    Collections.unmodifiableMap(new LinkedHashMap<>(Map.ofEntries(
      Map.entry("Bank booth", "Bank"),
      Map.entry("Bank chest", "Use"),
      Map.entry("Bank Deposit Box", "Deposit"),
      Map.entry("Bank Deposit Chest", "Deposit")
    )));
  private final Set<Integer> bankables;
  private final Integer bankablesThreshold;
  private final Map<Integer, BankLocation> regionToBankMap;

  private final InventoryHelper inventoryHelper;
  private final WalkHelper walkHelper;

  /**
   * @param script
   * @param bankLocations
   * @param bankables
   * @param bankablesThreshold Having this many bankables is a requirement for this task to execute. Leave unspecified
   *                           or as null to only attempt banking when the inventory is full.
   */
  public BankDepositTask(Script script, Set<BankLocation> bankLocations, Set<Integer> bankables, Integer bankablesThreshold) {
    super(script);

    this.bankables = bankables;
    this.bankablesThreshold = bankablesThreshold;
    regionToBankMap = BankLocation.getRegionToBankMap(bankLocations);

    inventoryHelper = new InventoryHelper(script, bankables);
    walkHelper = new WalkHelper(script);
  }
  public BankDepositTask(Script script, Set<BankLocation> bankLocations, Set<Integer> bankables) {
    this(script, bankLocations, bankables, null);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();
    if (!inventorySnapshot.containsAny(bankables)) return false;

    Bank bank = script.getWidgetManager().getBank();
    DepositBox depositBox = script.getWidgetManager().getDepositBox();
    if (bank.isVisible() || depositBox.isVisible()) return true;

    for (String bankDepositName : BANK_DEPOSIT_TO_MENU_ITEM_MAP.keySet()) {
      RSObject bankDepositObject = script.getObjectManager().getClosestObject(bankDepositName);
      if (bankDepositObject != null &&
        bankDepositObject.isInteractableOnScreen() &&
        bankDepositObject.getTileDistance() < 5) {
        return true;
      }
    }

    if (bankablesThreshold != null) {
      return inventorySnapshot.getAmount(bankables) >= bankablesThreshold;
    }

    return inventorySnapshot.isFull();
  }

  @Override
  public boolean execute() {
    Bank bank = script.getWidgetManager().getBank();
    DepositBox depositBox = script.getWidgetManager().getDepositBox();

    // If interfaces aren't already open, try to walk and interact with some type of bank deposit object
    if (!bank.isVisible() && !depositBox.isVisible()) {
      Integer regionId = script.getWorldPosition().getRegionID();
      BankLocation bankLocation = regionToBankMap.get(regionId);

      boolean interacted = false;
      for (String bankDepositName : BANK_DEPOSIT_TO_MENU_ITEM_MAP.keySet()) {
        String menuItem = BANK_DEPOSIT_TO_MENU_ITEM_MAP.get(bankDepositName);
        WorldPosition approximateLocation = bankLocation.approximatePosition;
        interacted = walkHelper.walkToAndInteract(bankDepositName, menuItem, approximateLocation);
        if (interacted) break;
      }
      if (!interacted) {
        script.log(getClass(), "Failed to walk and interact with any bank deposit object");
        return false;
      }

      if (!script.submitHumanTask(() -> {
        WidgetManager widgetManager = script.getWidgetManager();
        return widgetManager.getBank().isVisible() || widgetManager.getDepositBox().isVisible();
      }, 3_000)) {
        script.log(getClass(), "Failed to open bank deposit object interface");
        return false;
      }
    }

    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();

    if (bank.isVisible()) {
      for (Integer itemId : bankables) {
        if (!bank.deposit(itemId, inventorySnapshot.getAmount(itemId))) {
          script.log(getClass(), "Failed to deposit item " + itemId + " to bank");
        }
      }
      return true;
    } else if (depositBox.isVisible()) {
      for (Integer itemId : bankables) {
        if (!depositBox.deposit(itemId, inventorySnapshot.getAmount(itemId))) {
          script.log(getClass(), "Failed to deposit item " + itemId + " to deposit box");
        }
      }
      return true;
    }

    script.log(getClass(), "Unexpected failure when depositing items");
    return false;
  }
}

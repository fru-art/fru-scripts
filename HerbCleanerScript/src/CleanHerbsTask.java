import com.osmb.api.ui.bank.Bank;
import helper.InventoryHelper;
import task.Task;

public class CleanHerbsTask extends Task {
  private final HerbCleanerScript script;

  private final InventoryHelper inventoryHelper;

  public CleanHerbsTask(HerbCleanerScript script) {
    super(script);
    this.script = script;

    inventoryHelper = new InventoryHelper(script, HerbCleanerScript.GRIMY_HERBS);
  }

  @Override
  public boolean canExecute() {
    return this.inventoryHelper.getSnapshot().containsAny(HerbCleanerScript.GRIMY_HERBS);
  }

  @Override
  public boolean execute() {
    Bank bank = script.getWidgetManager().getBank();
    if (bank != null && bank.isVisible()) {
      bank.close();
      script.pollFramesUntil(() -> !bank.isVisible(), 3_600);
    }

    this.inventoryHelper.getSnapshot().getItem(HerbCleanerScript.GRIMY_HERBS).interact();

    int initialCount = inventoryHelper.getSnapshot().getAmount(HerbCleanerScript.GRIMY_HERBS);
    boolean herbsChanged = script.pollFramesUntil(
      () -> inventoryHelper.getSnapshot().getAmount(HerbCleanerScript.GRIMY_HERBS) < initialCount,
      3_600);
    if (!herbsChanged) return false;

    boolean herbsCleaned = script.pollFramesUntil(
      () -> !inventoryHelper.getSnapshot().containsAny(HerbCleanerScript.GRIMY_HERBS),
      initialCount * 1_200 + 600);
    return !herbsCleaned;
  }
}

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.HashSet;
import java.util.List;

public class ProcessAtObjectTask extends Task {
  private final WorldPosition approximatePosition;
  private final List<Integer> itemsToProcess;
  private final String menuItem;
  private final boolean needsDialogue;
  private final String objectName;

  private final InventoryHelper  inventoryHelper;
  private final WaitHelper waitHelper;
  private final WalkHelper walkHelper;

  // TODO: Fix up, should take in objects and items
  public ProcessAtObjectTask(
    Script script, String objectName, String menuItem, WorldPosition approximatePosition, List<Integer> itemsToProcess, Boolean needsDialogue) {
    super(script);

    this.approximatePosition = approximatePosition;
    this.itemsToProcess = itemsToProcess;
    this.menuItem = menuItem;
    this.needsDialogue = needsDialogue == null || needsDialogue;
    this.objectName = objectName;

    this.inventoryHelper = new InventoryHelper(script, new HashSet<>(itemsToProcess));
    this.waitHelper = new WaitHelper(script);
    this.walkHelper = new WalkHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = this.inventoryHelper.getSnapshot();
    RSObject object = script.getObjectManager().getClosestObject(objectName);

    if (object != null && object.distance() < 2 && inventorySnapshot.containsAny(itemsToProcess)) return true;

    return inventorySnapshot.isFull() && inventorySnapshot.containsAny(itemsToProcess);
  }

  @Override
  public boolean execute() {
    for (Integer itemToProcess : itemsToProcess) {
      if (!inventoryHelper.getSnapshot().contains(itemToProcess)) {
        continue;
      }

      // Start processing
      script.log(getClass(), "Starting processing " + itemToProcess);
      if (!walkHelper.walkToAndInteract(objectName, menuItem, approximatePosition)) {
        script.log(getClass(), "Failed to walk to and interact with object");
        return false;
      }

      if (needsDialogue) {
        if (!script.submitHumanTask(() -> {
          Dialogue dialogue = script.getWidgetManager().getDialogue();
          return dialogue != null && dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
        }, 3_000)) {
          script.log(getClass(), "Failed to open dialogue for " + objectName);
          return false;
        }

        Dialogue dialogue = script.getWidgetManager().getDialogue();
        assert dialogue != null;
        dialogue.selectItem(itemToProcess);
      }

      script.log(getClass(), "Waiting for processing " + itemToProcess + " to complete");
      waitHelper.waitForNoChange(
        "Process count " + itemToProcess,
        () -> inventoryHelper.getSnapshot().getAmount(itemToProcess),
        5_000, // Processing one item should take less than 5s
        Integer.MAX_VALUE, // Inventory is reliable so no timeout needed
        () -> inventoryHelper.getSnapshot().getAmount(itemToProcess) == 0);
    }

    return true;
  }
}

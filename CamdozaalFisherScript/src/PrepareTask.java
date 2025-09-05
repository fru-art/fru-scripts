import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import helper.InventoryHelper;
import helper.ObjectHelper;
import helper.WaitHelper;
import task.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrepareTask extends Task {
  // List for humanized preparation order
  private static final List<Integer> PREPARABLES = List.copyOf(List.of(
    ItemID.RAW_GUPPY,
    ItemID.RAW_CAVEFISH,
    ItemID.RAW_TETRA,
    ItemID.RAW_CATFISH));
  private static final Set<Integer> preparablesSet = new HashSet<>(PREPARABLES);

  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;
  private final WaitHelper waitHelper;

  public PrepareTask(Script script) {
    super(script);

    Set<Integer> searchables = new HashSet<>(preparablesSet);
    searchables.add(ItemID.KNIFE);
    inventoryHelper = new InventoryHelper(script, searchables);
    objectHelper = new ObjectHelper(script);
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();

    boolean hasKnife = inventorySnapshot.contains(ItemID.KNIFE);
    boolean hasPreparables = inventorySnapshot.containsAny(preparablesSet);
    boolean inventoryIsFull = inventorySnapshot.isFull();

    RSObject preparationTable = script.getObjectManager().getClosestObject("Preparation table");
    boolean isNextToPreparationTable = preparationTable != null && preparationTable.distance(script.getWorldPosition()) <= 2;

    return (inventoryIsFull || isNextToPreparationTable) && hasKnife && hasPreparables;
  }

  @Override
  public boolean execute() {
    for (Integer itemToBePrepared : PREPARABLES) {
      if (!inventoryHelper.getSnapshot().contains(itemToBePrepared)) {
        continue;
      }

      // Start preparing
      script.log(getClass(), "Starting preparing " + itemToBePrepared);
      if (!objectHelper.walkToAndTap(
        "Preparation table",
        new WorldPosition(2935, 5771, 0),
        "Prepare-fish")) {
        return false;
      }

      if (!script.submitHumanTask(() -> {
        Dialogue dialogue = script.getWidgetManager().getDialogue();
        return dialogue != null && dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
      }, 3_000)) {
        script.log(getClass(), "Failed to open dialogue for Preparation table");
        return false;
      }

      Dialogue dialogue = script.getWidgetManager().getDialogue();
      assert dialogue != null;
      dialogue.selectItem(itemToBePrepared);

      script.log(getClass(), "Waiting for preparing " + itemToBePrepared + " to complete");
      waitHelper.waitForNoChange(
        "Prepare count " + itemToBePrepared,
        () -> inventoryHelper.getSnapshot().getAmount(itemToBePrepared),
        5_000, // Preparing one item should take less than 5s
        40_000, // Preparing one inventory should take less than 40s
        () -> inventoryHelper.getSnapshot().getAmount(itemToBePrepared) == 0);
    }

    return true;
  }
}
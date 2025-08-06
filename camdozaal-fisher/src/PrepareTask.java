import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Collections;
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

  private final ScriptHelpers scriptHelpers;

  public PrepareTask(Script script) {
    super(script);
    this.scriptHelpers = new ScriptHelpers(script);
  }

  @Override
  public boolean canExecute() {
    boolean canReachPreparationTable =
      script.getObjectManager().getClosestObject("Preparation table").canReach();
    boolean hasKnife = script.getWidgetManager().getInventory().search(Set.of(ItemID.KNIFE)).contains(ItemID.KNIFE);
    boolean hasPreparables =
      script.getWidgetManager().getInventory().search(new HashSet<>(PREPARABLES)).containsAny(PREPARABLES);
    boolean inventoryIsFull = script.getWidgetManager().getInventory().search(Collections.emptySet()).isFull();


    return (inventoryIsFull || canReachPreparationTable) && hasKnife && hasPreparables;
  }

  @Override
  public boolean execute() {
    RSObject preparationTable = script.getObjectManager().getClosestObject("Preparation table");
    if (preparationTable == null) {
      script.log(getClass(), "Failed to find preparation table");
      return false;
    }

    // Walk to preparation table if not visible
    if (!preparationTable.isInteractableOnScreen()) {
      double distanceToPreparationTable = preparationTable.distance();
      script.getWalker().walkTo(preparationTable);
      if (!script.submitHumanTask(preparationTable::canReach, (int) (distanceToPreparationTable * 1_000))) {
        script.log(getClass(), "Failed to reach preparation table");
        return false;
      }
    }

    for (Integer itemToBePrepared : PREPARABLES) {
      if (!script.getWidgetManager().getInventory().search(Set.of(itemToBePrepared)).contains(itemToBePrepared)) {
        continue;
      }

      // Start preparing
      script.log(getClass(), "Starting preparing " + itemToBePrepared);
      preparationTable.interact("Prepare-fish");
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


      // Wait for preparing to stop
      // - Preparing one item should take less than 5s
      // - Preparing one inventory should take less than 40s
      script.log(getClass(), "Waiting for preparing " + itemToBePrepared + " to complete");
      scriptHelpers.waitForNoXp(5_000, 40_000, () -> {
        // Inventory no longer contains item
        if (!script.getWidgetManager().getInventory().search(Set.of(itemToBePrepared)).contains(itemToBePrepared)) {
          script.log(getClass(), "Preparing " + itemToBePrepared + " completed due no more items");
          return true;
        }
        return false;
      });
    }

    return true;
  }
}
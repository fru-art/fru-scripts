import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OfferTask extends Task {
  // List for humanized offering order
  private static final List<Integer> OFFERABLES = List.copyOf(List.of(
    ItemID.GUPPY,
    ItemID.CAVEFISH,
    ItemID.TETRA,
    ItemID.CATFISH));

  private final ScriptHelpers scriptHelpers;

  public OfferTask(Script script) {
    super(script);
    this.scriptHelpers = new ScriptHelpers(script);
  }

  @Override
  public boolean canExecute() {
    boolean hasOfferables =
      script.getWidgetManager().getInventory().search(new HashSet<>(OFFERABLES)).containsAny(OFFERABLES);
    boolean inventoryIsFull = script.getWidgetManager().getInventory().search(Collections.emptySet()).isFull();
    boolean isNextToAltar =
      script.getObjectManager().getClosestObject("Altar").distance() <= 3;

    return (inventoryIsFull || isNextToAltar) && hasOfferables;
  }


  @Override
  public boolean execute() {
    RSObject altar = script.getObjectManager().getClosestObject("Altar");
    if (altar == null) {
      script.log(getClass(), "Failed to find altar");
      return false;
    }

    // Walk to altar if not visible
    if (!altar.isInteractableOnScreen()) {
      double distanceToAltar = altar.distance();
      script.getWalker().walkTo(altar);
      if (!script.submitHumanTask(altar::canReach, (int) (distanceToAltar * 1_000))) {
        script.log(getClass(), "Failed to reach altar");
        return false;
      }
    }

    for (Integer itemToBeOffered : OFFERABLES) {
      if (!script.getWidgetManager().getInventory().search(Set.of(itemToBeOffered)).contains(itemToBeOffered)) {
        continue;
      }

      // Start offering
      script.log(getClass(), "Starting offering " + itemToBeOffered);
      altar.interact("Offer-fish");
      if (!script.submitHumanTask(() -> {
        Dialogue dialogue = script.getWidgetManager().getDialogue();
        return dialogue != null && dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
      }, 3_000)) {
        script.log(getClass(), "Failed to open dialogue for altar");
        return false;
      }

      Dialogue dialogue = script.getWidgetManager().getDialogue();
      assert dialogue != null;
      dialogue.selectItem(itemToBeOffered);

      // Wait for offering to stop
      // - Offering one item should take less than 5s
      // - Offering one inventory should take less than 40s
      script.log(getClass(), "Waiting for offering " + itemToBeOffered + " to complete");
      scriptHelpers.waitForNoXp(5_000, 40_000, () -> {
        // Inventory no longer contains item
        if (!script.getWidgetManager().getInventory().search(Set.of(itemToBeOffered)).contains(itemToBeOffered)) {
          script.log(getClass(), "Offering " + itemToBeOffered + " completed due no more items");
          return true;
        }
        return false;
      });
    }

    return true;
  }
}

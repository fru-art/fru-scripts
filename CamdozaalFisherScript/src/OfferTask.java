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

public class OfferTask extends Task {
  // List for humanized offering order
  private static final List<Integer> OFFERABLES = List.copyOf(List.of(
    ItemID.GUPPY,
    ItemID.CAVEFISH,
    ItemID.TETRA,
    ItemID.CATFISH));
  private static final Set<Integer> offerablesSet = new HashSet<>(OFFERABLES);

  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;
  private final WaitHelper waitHelper;

  public OfferTask(Script script) {
    super(script);

    inventoryHelper = new InventoryHelper(script, offerablesSet);
    objectHelper = new ObjectHelper(script);
    waitHelper = new WaitHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult inventorySnapshot = inventoryHelper.getSnapshot();

    boolean hasOfferables = inventorySnapshot.containsAny(offerablesSet);
    boolean inventoryIsFull = inventorySnapshot.isFull();

    RSObject altar = script.getObjectManager().getClosestObject(script.getWorldPosition(), "Altar");
    boolean isNextToAltar = altar != null && altar.distance(script.getWorldPosition()) <= 2;

    return (inventoryIsFull || isNextToAltar) && hasOfferables;
  }


  @Override
  public boolean execute() {
    for (Integer itemToBeOffered : OFFERABLES) {
      if (!inventoryHelper.getSnapshot().contains(itemToBeOffered)) {
        continue;
      }

      // Start offering
      script.log(getClass(), "Starting offering " + itemToBeOffered);
      if (!objectHelper.walkToAndTap(
        "Altar",
        new WorldPosition(2936, 5771, 0),
        "Offer-fish"
        )) {
        return false;
      }

      if (!script.submitHumanTask(() -> {
        Dialogue dialogue = script.getWidgetManager().getDialogue();
        return dialogue != null && dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
      }, 3_000, false, true)) {
        script.log(getClass(), "Failed to open dialogue for altar");
        return false;
      }

      Dialogue dialogue = script.getWidgetManager().getDialogue();
      assert dialogue != null;
      dialogue.selectItem(itemToBeOffered);

      script.log(getClass(), "Waiting for offering " + itemToBeOffered + " to complete");
      waitHelper.waitForNoChange(
        "Offer count " + itemToBeOffered,
        () -> inventoryHelper.getSnapshot().getAmount(itemToBeOffered),
        5_000, // Offering one item should take less than 5s
        40_000, // Offering one inventory should take less than 40s
        () -> inventoryHelper.getSnapshot().getAmount(itemToBeOffered) == 0);
    }

    return true;
  }
}

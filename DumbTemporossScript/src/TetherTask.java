import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.utils.UIResultList;
import helper.DetectionHelper;
import helper.InventoryHelper;
import helper.ObjectHelper;
import task.Task;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class TetherTask extends Task {
  private final TemporossScript script;

  private final DetectionHelper detectionHelper;
  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;

  public TetherTask(TemporossScript script) {
    super(script);
    this.script = script;

    detectionHelper = new DetectionHelper(script);
    inventoryHelper = new InventoryHelper(script, Set.of(ItemID.ROPE, ItemID.HAMMER));
    objectHelper = new ObjectHelper(script);
  }

  @Override
  public boolean canExecute() {
    Island island = script.getIsland();
    if (island == null) return false;

    if (!inventoryHelper.getSnapshot().contains(ItemID.ROPE)) return false;

    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    if (chatboxText == null) return false;

    for (String chatboxLine : chatboxText) {
      if (chatboxLine == null) continue;
      if (chatboxLine.contains("rope keeps") ||
        chatboxLine.contains("wave slams")) return false;
      if (chatboxLine.contains("colossal wave")) return true;
    }

    return false;
  }

  @Override
  public boolean execute() {
    WorldPosition position = script.getWorldPosition();

    List<RSObject> tetherables = objectHelper.getNamedObjects(List.of("Totem pole", "Mast"))
      .stream()
      .filter(object -> script.getIsland(object.getWorldPosition()) == script.getIsland())
      .sorted(Comparator.comparingInt(object -> object.getTileDistance(position)))
      .toList();

    if (tetherables.isEmpty()) {
      script.log(getClass(), "Failed to find tetherable");
      return false;
    }

    RSObject tetherable = tetherables.get(0);
    assert tetherable != null;

    objectHelper.walkTo(tetherable);
    assert tetherable.isInteractableOnScreen();

    AtomicBoolean tethered = new AtomicBoolean(false);
    while (canExecute() && !tethered.get()) {
      tetherable.interact(menuEntries -> {
        for (MenuEntry menuEntry : menuEntries) {
          String entryText = menuEntry.getRawText().toLowerCase();
          if (menuEntry.getRawText().toLowerCase().contains("repair") &&
            inventoryHelper.getSnapshot().contains(ItemID.HAMMER)) {
            return menuEntry;
          }

          if (entryText.contains("tether")) {
            tethered.set(true);
            return menuEntry;
          }
        }
        return null;
      });
    }

    script.submitHumanTask(() -> !canExecute(), 10_000);
    script.log(getClass(), "Finished tethering");

    if (didSucceed()) {
      script.log(getClass(), "Waiting for auto untether");
      script.submitHumanTask(() -> false, 1_800); // TODO: Improve untether detection
    }

    return true;
  }

  private boolean didSucceed() {
    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    if (chatboxText == null) return false;

    for (String chatboxLine : chatboxText) {
      if (chatboxLine == null) continue;
      if (chatboxLine.contains("rope keeps")) return true;
      if (chatboxLine.contains("colossal wave") ||
        chatboxLine.contains("wave slams")) return false;
    }

    return false;
  }
}

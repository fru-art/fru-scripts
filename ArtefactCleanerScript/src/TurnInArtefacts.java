import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import helper.InventoryHelper;
import helper.WaitHelper;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import task.Task;
import task.TaskScript;

public class TurnInArtefacts
extends Task {
    private final InventoryHelper inventoryHelper;
    private final WaitHelper waitHelper;

    public TurnInArtefacts(TaskScript script) {
        super(script);
        this.inventoryHelper = new InventoryHelper((Script)script, Stream.concat(ArtefactCleanerScript.ARTEFACTS.stream(), Stream.of(4447, 11175)).collect(Collectors.toSet()));
        this.waitHelper = new WaitHelper(script);
    }

    @Override
    public boolean canExecute() {
        ItemGroupResult snapshot = this.inventoryHelper.getSnapshot();
        return snapshot.containsAny(ArtefactCleanerScript.ARTEFACTS) && !snapshot.contains(11175) && !snapshot.containsAny(new int[]{4447});
    }

    @Override
    public boolean execute() {
        RSObject crate = this.script.getObjectManager().getClosestObject(this.script.getWorldPosition(), new String[]{"Storage crate"});
        if (crate == null) {
            this.script.log(this.getClass(), "Failed to find crate");
            return false;
        }
        int initialDistance = crate.getTileDistance(this.script.getWorldPosition());
        if (!crate.interact(new String[]{"Add finds"})) {
            this.script.log(this.getClass(), "Failed to interact with crate");
            return false;
        }
        this.waitHelper.waitForNoChange("position", () -> ((TaskScript)this.script).getWorldPosition(), 600, initialDistance * 600 + 600);
        this.waitHelper.waitForNoChange("artefact-count", () -> this.inventoryHelper.getSnapshot().getAmount(ArtefactCleanerScript.ARTEFACTS), 3600, Integer.MAX_VALUE, () -> !this.inventoryHelper.getSnapshot().containsAny(ArtefactCleanerScript.ARTEFACTS));
        return this.inventoryHelper.getSnapshot().containsAny(ArtefactCleanerScript.ARTEFACTS);
    }
}

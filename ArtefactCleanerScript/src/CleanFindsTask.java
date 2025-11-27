import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import helper.InventoryHelper;
import helper.WaitHelper;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import task.Task;
import task.TaskScript;

public class CleanFindsTask
extends Task {
    private final InventoryHelper inventoryHelper;
    private final WaitHelper waitHelper;

    public CleanFindsTask(TaskScript script) {
        super(script);
        this.inventoryHelper = new InventoryHelper((Script)script, Stream.concat(ArtefactCleanerScript.TOOLS.stream(), Stream.of(Integer.valueOf(11175))).collect(Collectors.toSet()));
        this.waitHelper = new WaitHelper(script);
    }

    @Override
    public boolean canExecute() {
        ItemGroupResult snapshot = this.inventoryHelper.getSnapshot();
        return snapshot.containsAll(ArtefactCleanerScript.TOOLS) && snapshot.contains(11175) && snapshot.isFull();
    }

    @Override
    public boolean execute() {
        RSObject table = this.script.getObjectManager().getClosestObject(this.script.getWorldPosition(), new String[]{"Specimen table"});
        if (table == null) {
            this.script.log(this.getClass(), "Failed to find table");
            return false;
        }
        int initialDistance = table.getTileDistance(this.script.getWorldPosition());
        if (!table.interact(new String[]{"Clean"})) {
            this.script.log(this.getClass(), "Failed to interact with table");
            return false;
        }
        this.waitHelper.waitForNoChange("position", () -> ((TaskScript)this.script).getWorldPosition(), 600, initialDistance * 600 + 600);
        this.waitHelper.waitForNoChange("find-count", () -> this.inventoryHelper.getSnapshot().getAmount(new int[]{11175}), 6000, Integer.MAX_VALUE, () -> !this.inventoryHelper.getSnapshot().contains(11175));
        return !this.inventoryHelper.getSnapshot().contains(11175);
    }
}

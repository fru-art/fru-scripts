import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;

import java.util.Set;

public class DropTask extends Task {
  private static final Set<Integer> DROPPABLES = Set.copyOf(Set.of(
//    ItemID.BARRONITE_HANDLE, // TODO: Remove once banking is implemented
    ItemID.FROG_SPAWN,
    ItemID.RUINED_CAVEFISH,
    ItemID.RUINED_GUPPY,
    ItemID.RUINED_TETRA,
    ItemID.RUINED_CATFISH));

  public DropTask(Script script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    return script.getWidgetManager().getInventory().search(DROPPABLES).containsAny(DROPPABLES);
  }

  @Override
  public boolean execute() {
    return script.getWidgetManager().getInventory().dropItems(DROPPABLES);
  }
}

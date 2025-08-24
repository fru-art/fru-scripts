import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.List;

@ScriptDefinition(
  author = "fru",
  name = "Goblin Slayer",
  description = "Kills goblins and picks up some things",
  skillCategory = SkillCategory.COMBAT,
  version = 1.3
)

public class GoblinSlayerScript extends SequenceTaskScript {
  public GoblinSlayerScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  protected List<Task> getTaskList() {
    return List.of(
      new WalkTask(this, new WorldPosition(3252, 3234, 0), 7),
      new KillGoblinTask(this)
    );
  }

  @Override
  protected List<Integer> getRequiredRegions() {
    return List.of(12850, 13106);
  }
}

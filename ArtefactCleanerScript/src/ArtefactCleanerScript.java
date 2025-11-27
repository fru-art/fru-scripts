import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import task.FirstMatchTaskScript;
import task.Task;

@ScriptDefinition(author="fru", name="Artefact Cleaner", description="for slow XP lamps in the Varrock Museum", skillCategory=SkillCategory.OTHER, version=1.0)
public class ArtefactCleanerScript
extends FirstMatchTaskScript {
    public static final Set<Integer> ARTEFACTS = Set.of(Integer.valueOf(11176), Integer.valueOf(11177), Integer.valueOf(11183), Integer.valueOf(11178));
    public static final Set<Integer> DROPS = new HashSet<Integer>(Set.of(11180, 11181, 532, 526, 1923, 687, 1469, 9420, 11195, 453, 617, 995, 6964, 8890, 436, 40, 9140, 1203, 807, 863, 440, 447, 11179, 11182, 1931, 438, 1627, 1625, 9440));
    public static final Set<Integer> INTERACTABLES = new HashSet<Integer>(Set.of());
    public static final Set<Integer> TOOLS = Set.of(Integer.valueOf(675), Integer.valueOf(670), Integer.valueOf(676));
    public static final Set<Integer> allItems = Stream.of(Set.of(Integer.valueOf(4447), Integer.valueOf(11175)), ARTEFACTS, DROPS, INTERACTABLES, TOOLS).flatMap(Collection::stream).collect(Collectors.toSet());
    public final ArtefactCleanerScriptOptions scriptOptions = new ArtefactCleanerScriptOptions(this);

    public ArtefactCleanerScript(Object scriptCore) {
        super(scriptCore);
    }

    public void onStart() {
        super.onStart();
        this.scriptOptions.show();
        if (this.scriptOptions.getSelectedSkillSprite() == -1) {
            Object skill = this.scriptOptions.skillDropdown.comboBox.getValue();
            this.log(((Object)((Object)this)).getClass(), "Failed to retrieve sprite for selected skill: " + String.valueOf(skill));
            this.stop();
            return;
        }
        if (this.scriptOptions.buryBigBonesCheckbox.isSelected()) {
            DROPS.remove(532);
            INTERACTABLES.add(532);
        }
        if (this.scriptOptions.buryBonesCheckbox.isSelected()) {
            DROPS.remove(526);
            INTERACTABLES.add(526);
        }
        if (this.scriptOptions.equipIronBoltsCheckbox.isSelected()) {
            DROPS.remove(9140);
            INTERACTABLES.add(9140);
        }
        if (this.scriptOptions.equipIronKnivesCheckbox.isSelected()) {
            DROPS.remove(863);
            INTERACTABLES.add(863);
        }
    }

    @Override
    protected List<Task> getTaskList() {
        return List.of(new OpenLampsTask(this), new CleanFindsTask(this), new TurnInArtefacts(this), new CleanInventoryTask(this), new TakeFindsTask(this));
    }

    @Override
    protected List<Integer> getRequiredRegions() {
        return List.of(Integer.valueOf(12853), Integer.valueOf(13109));
    }
}

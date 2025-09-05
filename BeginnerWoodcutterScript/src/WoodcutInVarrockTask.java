import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.PixelAnalyzer;
import helper.DrawHelper;
import helper.InventoryHelper;
import helper.ObjectHelper;
import task.Task;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WoodcutInVarrockTask extends Task {
  private static final RectangleArea VARROCK_TREES = new RectangleArea(3158, 3406, 12, 17, 0);

  private final BeginnerWoodcutterScriptOptions scriptOptions;

  private final DrawHelper drawHelper;
  private final InventoryHelper inventoryHelper;
  private final ObjectHelper objectHelper;

  public WoodcutInVarrockTask(Script script, BeginnerWoodcutterScriptOptions scriptOptions) {
    super(script);
    this.scriptOptions = scriptOptions;

    drawHelper = new DrawHelper(script);
    inventoryHelper = new InventoryHelper(script, scriptOptions.cuttableItems);
    objectHelper = new ObjectHelper(script);
  }

  @Override
  public boolean canExecute() {
    List<RSObject> trees = getUncutTrees();
    if (trees.isEmpty()) {
      script.log(getClass(), "Failed to find trees");
      return false;
    }

    return true;
  }

  @Override
  public boolean execute() {
    List<RSObject> trees = getUncutTrees();
    if (trees.isEmpty()) {
      script.log(getClass(), "Failed to find trees");
      return false;
    }
    script.log("Found " + trees.size() + " uncut trees");

    for (RSObject tree : trees) {
      Polygon treePolygon = tree.getConvexHull();
      if (treePolygon == null || !script.getFinger().tapGameScreen(treePolygon)) {
        continue;
      }

      drawHelper.drawPolygon(tree.getWorldPosition().toString(), treePolygon, Color.CYAN);
      double initialDistance = tree.getTileDistance(script.getWorldPosition());

      if (!script.submitHumanTask(() -> {
        if (!getUncutTrees().contains(tree)) return false;
        return tree.getTileDistance(script.getWorldPosition()) <= 1;
      }, (int) (initialDistance * 1_000))) {
        script.log(getClass(), "Failed to get to tree");
        return false;
      }

      while (getUncutTrees().contains(tree)) {
        int initialItemCount = inventoryHelper.getSnapshot().getAmount(scriptOptions.cuttableItems);
        if (!script.submitHumanTask(() -> {
          if (inventoryHelper.getSnapshot().getAmount(scriptOptions.cuttableItems) > initialItemCount) {
            return true;
          }
          return !getUncutTrees().contains(tree);
        }, 8_000)) {
          script.log(getClass(), "Failed to chop tree");
          return false;
        }
      }

      return true;
    }

    script.log(getClass(), "Failed to tap any trees");
    return false;
  }

  private List<RSObject> getUncutTrees() {
    List<RSObject> trees = objectHelper.getNamedObjects(scriptOptions.cuttableTreeNames).stream()
      .filter(tree -> VARROCK_TREES.contains(tree.getWorldPosition()))
      .toList();

    Map<RSObject, PixelAnalyzer.RespawnCircle> treeToRespawnCircleMap = script
      .getPixelAnalyzer()
      .getRespawnCircleObjects(trees, PixelAnalyzer.RespawnCircleDrawType.CENTER, 0, 7);
    Set<RSObject> cutTrees = treeToRespawnCircleMap.keySet();

    WorldPosition position = script.getWorldPosition();
    List<RSObject> uncutTrees = trees.stream()
      .filter(tree -> !cutTrees.contains(tree))
      .sorted(Comparator.comparing(RSObject::getName) // Heuristic to prioritize oak trees
        .thenComparingDouble(tree -> tree.distance(position))) // Much faster heuristic for tile distance
      .toList();

    for (RSObject tree : cutTrees) {
      Polygon treePolygon = tree.getConvexHull();
      if (treePolygon == null) continue;;
      drawHelper.drawPolygon(tree.getWorldPosition().toString(), treePolygon, Color.RED);
    }
    for (RSObject tree : uncutTrees) {
      Polygon treePolygon = tree.getConvexHull();
      if (treePolygon == null) continue;
      drawHelper.drawPolygon(tree.getWorldPosition().toString(), treePolygon, Color.GREEN);
    }

    return uncutTrees;
  }
}

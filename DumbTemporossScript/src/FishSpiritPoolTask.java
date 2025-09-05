import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.PixelCluster.ClusterQuery;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.walker.WalkConfig;
import helper.DetectionHelper;
import task.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class FishSpiritPoolTask extends Task {
  private static final Map<Island, WorldPosition> NEAR_SPIRIT_POOL = Map.of(
    Island.NORTH, new WorldPosition(3047, 2984, 0),
    Island.SOUTH, new WorldPosition(3047, 2970, 0)
  );
  private static final Map<Island, WorldPosition> SPIRIT_POOL = Map.of(
    Island.NORTH, new WorldPosition(3047, 2982, 0),
    Island.SOUTH, new WorldPosition(3047, 2972, 0)
  );
  private static final SearchablePixel[] ENERGY_PIXELS = {
    new SearchablePixel(-10888216, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
    new SearchablePixel(-16724768, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
  };

  private static final ClusterQuery energyBarQuery = new ClusterQuery(1, 1024, Stream.concat(
    Stream.of(
      new SearchablePixel(-6835531, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
      new SearchablePixel(-10384240, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)
    ),
    Arrays.stream(ENERGY_PIXELS)
  ).toArray(SearchablePixel[]::new));
  // Only use this query within the bounds of the energy bar
  private static final ClusterQuery energyQuery = new ClusterQuery(1, 0, ENERGY_PIXELS);

  private final DumbTemporossScript script;

  private final DetectionHelper detectionHelper;

  public FishSpiritPoolTask(DumbTemporossScript script) {
    super(script);
    this.script = script;

    detectionHelper = new DetectionHelper(script);
  }

  @Override
  public boolean canExecute() {
    Island island = script.getIsland();
    if (island == null) return false;

    Double energy = getEnergy();
    if (energy == null || energy > 0.25) return false;

    UIResultList<String> chatboxText = detectionHelper.getChatboxText();
    if (chatboxText == null) return false;

    for (String chatboxLine : chatboxText) {
      if (chatboxLine == null) continue;
      if (chatboxLine.contains("string wind") || chatboxLine.contains("colossal wave")) return false;
      if (chatboxLine.contains("is vulnerable")) return true;
    }

    return false;
  }

  @Override
  public boolean execute() {
    Island island = script.getIsland();
    assert island != null;

    WorldPosition nearSpiritPool = NEAR_SPIRIT_POOL.get(island);
    WorldPosition spiritPool = SPIRIT_POOL.get(island);
    if (nearSpiritPool == null || spiritPool == null) {
      script.log(getClass(), "Failed to find spirit pool for island " + island);
      return false;
    }

    WalkConfig walkConfig = new WalkConfig.Builder()
      .breakCondition(() -> getSpiritPoolPolygon() != null)
      .build();

    script.getWalker().walkTo(nearSpiritPool, walkConfig);

    Polygon spiritPoolPolygon = getSpiritPoolPolygon();
    if (spiritPoolPolygon == null) {
      script.log(getClass(), "Failed to walk to spirit pool");
      return false;
    }
    script.getFinger().tapGameScreen(spiritPoolPolygon);

    // TODO: Migrate to XP tracker check
    AtomicReference<Double> energy = new AtomicReference<>(null);

    do {
      energy.set(getEnergy());
      script.submitHumanTask(() -> false, 600); // 1 tick
      spiritPoolPolygon = getSpiritPoolPolygon();
      if (spiritPoolPolygon == null) continue;
      if (script.getFinger().tapGameScreen(spiritPoolPolygon, "Harpoon")) break;
    } while (canExecute() &&
      ((energy.get() != null && energy.get() < 0.05) || script.getWorldPosition().distanceTo(spiritPool) > 2));

    script.submitHumanTask(() -> {
      // Catch-all for completing minigame in case of extraneous game messages
      if (script.getIsland() == null) return true;

      UIResultList<String> chatboxText = detectionHelper.getChatboxText();
      if (chatboxText != null) {
        for (String chatboxLine : chatboxText) {
          if (chatboxLine == null) continue;
          if (chatboxLine.contains("string wind") || chatboxLine.contains("colossal wave")) return true;
          if (chatboxLine.contains("is vulnerable")) break;
        }
      }

      energy.set(getEnergy());
      return energy.get() != null && energy.get() > 0.95;
    }, 2 * 60_000);

    return true;
  }

  private Polygon getSpiritPoolPolygon() {
    Island island = script.getIsland();
    if (island == null) return null;

    WorldPosition spiritPool = SPIRIT_POOL.get(island);
    if (spiritPool == null) return null;

    RSTile tile = script.getSceneManager().getTile(spiritPool);
    if (!tile.isOnGameScreen()) return null;

    return tile.getTilePoly();
  }

  private Double getEnergy() {
    return script.getResource(energyBarQuery, energyQuery, Color.CYAN.darker());
  }
}

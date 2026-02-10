import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmbtoolkit.job.Job;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NOTE: This class was deconstructed and recovered from a JAR and also migrated from a legacy framework. Be wary if
 * using it as a reference.
 */
public class OpenLampsJob extends Job<ArtefactCleanerScript> {
  private final Set<Integer> items = Set.of(4447);

  public OpenLampsJob(ArtefactCleanerScript script) {
    super(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventoryVisible(items);
    return snapshot.containsAny(4447);
  }

  @Override
  public boolean execute() {
    int skillSprite = script.scriptOptions.getSelectedSkillSprite();
    AtomicReference<ItemGroupResult> atomicSnapshot =
      new AtomicReference<ItemGroupResult>(script.pollFramesUntilInventoryVisible(items));

    while (atomicSnapshot.get().containsAny(4447)) {
      int initialLampCount = atomicSnapshot.get().getAmount(4447);
      ItemSearchResult lamp = atomicSnapshot.get().getItem(4447);
      assert (lamp != null);
      script.log(this.getClass(), "Opening lamp in slot " + lamp.getSlot());
      if (!lamp.interact()) {
        script.log(this.getClass(), "Failed to interact with lamp in slot " + lamp.getSlot());
        return false;
      }
      AtomicReference<ImageSearchResult> atomicSkill = new AtomicReference<>(null);
      script.pollFramesHuman(
        () -> {
          atomicSkill.set(this.findSkill(skillSprite));
          return atomicSkill.get() != null;
        }, 3000, true);
      ImageSearchResult skill = atomicSkill.get();
      if (skill == null) {
        script.log(this.getClass(), "Failed to find skill in lamp UI (critical)");
        script.stop();
        return false;
      }
      script.getFinger().tap(skill.getBounds());
      AtomicReference<Rectangle> atomicLineBounds = new AtomicReference<>(null);
      script.pollFramesHuman(
        () -> {
          atomicLineBounds.set(this.findConfirmBounds());
          return atomicLineBounds.get() != null;
        }, 3000, true);
      Rectangle lineBounds = atomicLineBounds.get();

      Optional<PixelCluster> textCluster = script.findLargestCluster(
        lineBounds,
        new SearchablePixel[]{new SearchablePixel(-26593, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)},
        3,
        32);
      if (textCluster.isEmpty() || textCluster.get().getBounds() == null) {
        script.log(this.getClass(), "Failed to find confirm bounds");
        return false;
      }
      Rectangle textBounds = textCluster.get().getBounds();
      script.log(this.getClass(), "Lamp UI bounds" + getLampUiBounds());
      script.log(this.getClass(), "Line bounds" + lineBounds);
      script.log(this.getClass(), "Found confirm bounds: " + textBounds);
      script.getFinger().tap(textBounds);
      boolean rubbedLamp = script.pollFramesHuman(
        () -> {
          atomicSnapshot.set(script.pollFramesUntilInventoryVisible(items));
          return atomicSnapshot.get().getAmount(4447) < initialLampCount;
        }, 3_600, true);
      if (rubbedLamp) continue;
      script.log(this.getClass(), "Failed to reduce lamp count from " + initialLampCount);
      return false;
    }
    return script.pollFramesHuman(() -> !script.pollFramesUntilInventoryVisible(items).contains(4447), 1200, true);
  }

  private Rectangle findConfirmBounds() {
    for (Rectangle textBounds : script.getUtils().getTextBounds(this.getLampUiBounds(), 15, -26593)) {
      String text = script.getOCR().getText(Font.FONT_5676, textBounds, -26593);
      if (text == null || !text.startsWith("Confirm")) continue;
      return textBounds;
    }
    return null;
  }

  private ImageSearchResult findSkill(int spriteId) {
    SearchableImage searchable = new SearchableImage(
      script.getSpriteManager().getSprite(spriteId),
      new SingleThresholdComparator(3),
      ColorModel.RGB);
    return script.getImageAnalyzer().findLocation(this.getLampUiBounds(), searchable);
  }

  private Rectangle getLampUiBounds() {
    Rectangle screenBounds = script.getScreen().getBounds();
    return new Rectangle(
      screenBounds.x + 894 / 6,
      screenBounds.y + 640 / 3,
      Math.min(screenBounds.width, screenBounds.x + 894 / 6 * 4),
      Math.min(screenBounds.height, screenBounds.y + 640 / 6 * 4));
  }
}

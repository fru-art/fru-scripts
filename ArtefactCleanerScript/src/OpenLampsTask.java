import com.osmb.api.definition.SpriteDefinition;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.SpriteID;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;
import helper.DetectionHelper;
import helper.InventoryHelper;
import org.w3c.dom.css.Rect;
import task.Task;
import task.TaskScript;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class OpenLampsTask extends Task {
  private static final int CONFIRM_COLOR = -6710887;

  private final ArtefactCleanerScript script;

  private final InventoryHelper inventoryHelper;
  private final DetectionHelper detectionHelper;

  public OpenLampsTask(ArtefactCleanerScript script) {
    super(script);
    this.script = script;

    isCritical = true;

    this.inventoryHelper = new InventoryHelper(script, Set.of(ItemID.ANTIQUE_LAMP));
    this.detectionHelper = new DetectionHelper(script);
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = inventoryHelper.getSnapshot();
    return snapshot.containsAny(ItemID.ANTIQUE_LAMP);
  }

  @Override
  public boolean execute() {
    int skillSprite = script.scriptOptions.getSelectedSkillSprite();
    AtomicReference<ItemGroupResult> atomicSnapshot = new AtomicReference<>(inventoryHelper.getSnapshot());

    while (atomicSnapshot.get().containsAny(ItemID.ANTIQUE_LAMP)) {
      int initialLampCount = atomicSnapshot.get().getAmount(ItemID.ANTIQUE_LAMP);
      ItemSearchResult lamp = atomicSnapshot.get().getItem(ItemID.ANTIQUE_LAMP);
      assert lamp != null;

      script.log(getClass(), "Opening lamp in slot " + lamp.getSlot());
      if (!lamp.interact()) {
        script.log(getClass(), "Failed to interact with lamp in slot " + lamp.getSlot());
        return false;
      }

      AtomicReference<ImageSearchResult> atomicSkill = new AtomicReference<>(null);
      script.pollFramesHuman(() -> {
        atomicSkill.set(findSkill(skillSprite));
        return atomicSkill.get() != null;
      }, 4 * 600, true);
      ImageSearchResult skill = atomicSkill.get();

      if (skill == null) {
        script.log(getClass(), "Failed to find skill in lamp UI (critical)");
        script.stop();
        return false;
      }

      script.getFinger().tap(skill.getBounds());

      // Find and click on confirm
      AtomicReference<Rectangle> atomicLineBounds = new AtomicReference<>(null);
      script.pollFramesHuman(() -> {
        atomicLineBounds.set(findConfirmBounds());
        return atomicLineBounds.get() != null;
      }, 2 * 600, true);

      Rectangle lineBounds = atomicLineBounds.get();
      com.osmb.api.visual.PixelCluster textCluster = detectionHelper.getLargestCluster(
        lineBounds,
        new PixelCluster.ClusterQuery(3, 32, new SearchablePixel[]{
          new SearchablePixel(CONFIRM_COLOR, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)}));
      if (textCluster == null) return false;

      Rectangle textBounds = textCluster.getBounds();
      script.log(getClass(), "Found confirm bounds: " + textBounds);

      script.getFinger().tap(textBounds);
      boolean rubbedLamp = script.pollFramesHuman(() -> {
        atomicSnapshot.set(inventoryHelper.getSnapshot());
        return atomicSnapshot.get().getAmount(ItemID.ANTIQUE_LAMP) < initialLampCount;
      }, 4 * 600, true);
      if (!rubbedLamp) {
        script.log(getClass(), "Failed to reduce lamp count from " + initialLampCount);
        return false;
      }
    }

    return script.pollFramesHuman(
      () -> !inventoryHelper.getSnapshot().contains(ItemID.ANTIQUE_LAMP), 2 * 600, true);
  }

  private Rectangle findConfirmBounds() {
    Rectangle[] textBoundss = script.getUtils().getTextBounds(getLampUiBounds(), 15, CONFIRM_COLOR);

    for (Rectangle textBounds : textBoundss) {
      String text = script.getOCR().getText(Font.FONT_5676, textBounds, CONFIRM_COLOR);
      if (text != null && text.startsWith("Confirm")) {
        return textBounds;
      }
    }

    return null;
  }

  private ImageSearchResult findSkill(int spriteId) {
    SearchableImage searchable = new SearchableImage(
      script.getSpriteManager().getSprite(spriteId),
      new SingleThresholdComparator(3),
      ColorModel.RGB);

    return script.getImageAnalyzer().findLocation(getLampUiBounds(), searchable);
  }

  private Rectangle getLampUiBounds() {
    Rectangle screenBounds = script.getScreen().getBounds();
    return new Rectangle(
      screenBounds.x + screenBounds.width / 6,
      screenBounds.y + screenBounds.height / 6,
      screenBounds.width * 4 / 6,
      screenBounds.height * 4 / 6
    );
  }
}

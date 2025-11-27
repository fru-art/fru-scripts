import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import task.Task;

public class OpenLampsTask
extends Task {
    private static final int CONFIRM_COLOR = -26593;
    private final ArtefactCleanerScript script;
    private final InventoryHelper inventoryHelper;
    private final DetectionHelper detectionHelper;

    public OpenLampsTask(ArtefactCleanerScript script) {
        super(script);
        this.script = script;
        this.isCritical = true;
        this.retryLimit = 3;
        this.inventoryHelper = new InventoryHelper((Script)script, Set.of(Integer.valueOf(4447)));
        this.detectionHelper = new DetectionHelper(script);
    }

    @Override
    public boolean canExecute() {
        ItemGroupResult snapshot = this.inventoryHelper.getSnapshot();
        return snapshot.containsAny(new int[]{4447});
    }

    @Override
    public boolean execute() {
        int skillSprite = this.script.scriptOptions.getSelectedSkillSprite();
        AtomicReference<ItemGroupResult> atomicSnapshot = new AtomicReference<ItemGroupResult>(this.inventoryHelper.getSnapshot());
        while (atomicSnapshot.get().containsAny(new int[]{4447})) {
            int initialLampCount = atomicSnapshot.get().getAmount(new int[]{4447});
            ItemSearchResult lamp = atomicSnapshot.get().getItem(new int[]{4447});
            assert (lamp != null);
            this.script.log(this.getClass(), "Opening lamp in slot " + lamp.getSlot());
            if (!lamp.interact()) {
                this.script.log(this.getClass(), "Failed to interact with lamp in slot " + lamp.getSlot());
                return false;
            }
            AtomicReference<ImageSearchResult> atomicSkill = new AtomicReference<>(null);
            this.script.pollFramesHuman(() -> {
                atomicSkill.set(this.findSkill(skillSprite));
                return atomicSkill.get() != null;
            }, 3000, true);
            ImageSearchResult skill = atomicSkill.get();
            if (skill == null) {
                this.script.log(this.getClass(), "Failed to find skill in lamp UI (critical)");
                this.script.stop();
                return false;
            }
            this.script.getFinger().tap((Shape)skill.getBounds());
            AtomicReference<Rectangle> atomicLineBounds = new AtomicReference<>(null);
            this.script.pollFramesHuman(() -> {
                atomicLineBounds.set(this.findConfirmBounds());
                return atomicLineBounds.get() != null;
            }, 3000, true);
            Rectangle lineBounds = atomicLineBounds.get();
            PixelCluster textCluster = this.detectionHelper.getLargestCluster((Shape)lineBounds, new PixelCluster.ClusterQuery(3, 32, new SearchablePixel[]{new SearchablePixel(-26593, (ToleranceComparator)ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)}));
            if (textCluster == null) {
                return false;
            }
            Rectangle textBounds = textCluster.getBounds();
            this.script.log(this.getClass(), "Found confirm bounds: " + String.valueOf(textBounds));
            this.script.getFinger().tap((Shape)textBounds);
            boolean rubbedLamp = this.script.pollFramesHuman(() -> {
                atomicSnapshot.set(this.inventoryHelper.getSnapshot());
                return ((ItemGroupResult)atomicSnapshot.get()).getAmount(new int[]{4447}) < initialLampCount;
            }, 3000, true);
            if (rubbedLamp) continue;
            this.script.log(this.getClass(), "Failed to reduce lamp count from " + initialLampCount);
            return false;
        }
        return this.script.pollFramesHuman(() -> !this.inventoryHelper.getSnapshot().contains(4447), 1200, true);
    }

    private Rectangle findConfirmBounds() {
        Rectangle[] textBoundss;
        for (Rectangle textBounds : textBoundss = this.script.getUtils().getTextBounds(this.getLampUiBounds(), 15, -26593)) {
            String text = this.script.getOCR().getText(Font.FONT_5676, textBounds, new int[]{-26593});
            if (text == null || !text.startsWith("Confirm")) continue;
            return textBounds;
        }
        return null;
    }

    private ImageSearchResult findSkill(int spriteId) {
        SearchableImage searchable = new SearchableImage(this.script.getSpriteManager().getSprite(spriteId), (ToleranceComparator)new SingleThresholdComparator(3), ColorModel.RGB);
        return this.script.getImageAnalyzer().findLocation((Shape)this.getLampUiBounds(), new SearchableImage[]{searchable});
    }

    private Rectangle getLampUiBounds() {
        Rectangle screenBounds = this.script.getScreen().getBounds();
        return new Rectangle(screenBounds.x + screenBounds.width / 6, screenBounds.y + screenBounds.height / 6, screenBounds.width * 4 / 6, screenBounds.height * 4 / 6);
    }
}

import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ScriptHelpers {
  private final Script script;

  public ScriptHelpers(Script script) {
    this.script = script;
  }

  /**
   * This method waits for XP to stop changing (or some other success condition). This is a more robust way of detecting
   * if an activity has completed because OSMB may interrupt your activity with world hopping or other events.
   *
   * @param intervalMs Amount of time to wait between XP changes. If the time is exceeded, the activity is considered
   *                   to be successfully completed
   * @param timeoutMs Amount of time to wait overall. If the time is exceeded, the activity is considered to have failed
   *                  completed
   * @param successCondition Additional success condition where caller decides that waiting for XP to stop changing is
   *                         no longer necessary
   */
  public boolean waitForNoXp(int intervalMs, int timeoutMs, java.util.function.BooleanSupplier successCondition) {
    AtomicReference<Double> xp = new AtomicReference<>(readXp());
    AtomicLong xpChangeEpoch = new AtomicLong(System.currentTimeMillis());

    return script.submitHumanTask(() -> {
      if (successCondition.getAsBoolean()) return true;

      Double nextXp = readXp();
      if (Math.abs(nextXp - xp.get()) > 1) {
        long epoch = System.currentTimeMillis();
        script.log(
          getClass(),
          "XP changed: " + xp.get() + " -> " + nextXp + " @ " + epoch);
        xp.set(nextXp);
        xpChangeEpoch.set(epoch);
      } else if (System.currentTimeMillis() - xpChangeEpoch.get() > intervalMs) {
        script.log(getClass(), "No XP changed within " + intervalMs + "ms since " + xpChangeEpoch.get());
        return true;
      }

      return false;
    }, timeoutMs);
  }

  public Double readXp() {
    XPDropsComponent xpComponent = (XPDropsComponent) script.getWidgetManager().getComponent(XPDropsComponent.class);
    if (xpComponent == null) {
      script.log(getClass(), "Failed to find XP component");
      return null;
    }

    @SuppressWarnings("unchecked")
    ComponentSearchResult<Integer> result = (ComponentSearchResult<Integer>) xpComponent.getResult();
    if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
      script.log(getClass(), "Failed to get XP component");
      return null;
    }

    com.osmb.api.shape.Rectangle componentBounds = result.getBounds();
    com.osmb.api.shape.Rectangle xpTextRect = new Rectangle(componentBounds.x - 140, componentBounds.y - 1, 119, 38);
    String xpText = script.getOCR().getText(Font.SMALL_FONT, xpTextRect, Color.WHITE.getRGB());

    if (xpText == null || xpText.isBlank()) {
      script.log(getClass(), "Failed to find XP text");
      return null;
    }
    xpText = xpText.replaceAll("\\D", "");
    if (xpText.isEmpty()) {
      script.log(getClass(), "Failed to read XP text");
      return null;
    }

    try {
      double xp = Double.parseDouble(xpText);
      if (xp <= 0) return null;
      return xp;
    } catch (NumberFormatException e) {
      script.log(getClass(), "Failed to parse XP text: " + xpText);
      return null;
    }
  }
}

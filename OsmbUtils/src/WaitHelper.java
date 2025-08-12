import com.osmb.api.script.Script;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class WaitHelper {
  private final Script script;

  public WaitHelper(Script script) {
    this.script = script;
  }

  public <T> boolean waitForChange(String valueName, Supplier<T> valueSupplier, int timeoutMs) {
    AtomicReference<T> value = new AtomicReference<>(valueSupplier.get());
    script.log(getClass(), "Waiting for " +  valueName + " value " + value.get() + " to change");

    return script.submitHumanTask(() -> {
      T nextValue = valueSupplier.get();
      if (nextValue != value) {
        script.log(getClass(), valueName + " value successfully changed to " + nextValue);
        return true;
      }
      return false;
    }, timeoutMs);
  }

  public <T> boolean waitForNoChange(
    String valueName,
    Supplier<T> valueSupplier,
    int intervalMs,
    int timeoutMs,
    BooleanSupplier earlyExitSupplier,
    boolean ignoreHumanTasks) {
    AtomicReference<T> currentValue = new AtomicReference<>(valueSupplier.get());
    AtomicLong changeTime = new AtomicLong(System.currentTimeMillis());
    script.log(getClass(), "Waiting for " +  valueName + " value " + currentValue.get() + " to stop changing");

    return script.submitHumanTask(() -> {
      if (earlyExitSupplier.getAsBoolean()) return true;

      T nextValue = valueSupplier.get();
      if (currentValue.get() != nextValue) {
        long nextChangeTime = System.currentTimeMillis();
        long timeDiff = nextChangeTime - changeTime.get();
        script.log(getClass(), valueName + " value changed to " + nextValue + " after " + timeDiff  + " ms");
        currentValue.set(nextValue);
        changeTime.set(nextChangeTime);
        return false;
      }

      long currentTime = System.currentTimeMillis();
      long timeDiff = currentTime - changeTime.get();
      if (timeDiff > intervalMs) {
        script.log(getClass(), valueName + " value successfully stopped changing after " + timeDiff  + " ms");
        return true;
      }

      return false;
    }, timeoutMs, false, ignoreHumanTasks);
  }
  public <T> boolean waitForNoChange(
    String valueName,
    Supplier<T> valueSupplier,
    int intervalMs,
    int timeoutMs,
    BooleanSupplier earlyExitSupplier) {
    return waitForNoChange(valueName, valueSupplier, intervalMs, timeoutMs, earlyExitSupplier, false);
  }
  public <T> boolean waitForNoChange(String valueName, Supplier<T> valueSupplier, int intervalMs, int timeoutMs) {
    return waitForNoChange(valueName, valueSupplier, intervalMs, timeoutMs, () -> false, false);
  }
}

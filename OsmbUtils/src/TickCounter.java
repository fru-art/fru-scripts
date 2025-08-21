public class TickCounter {
  private static final long TICK_MS = 600;

  public long lastSyncTickId = 0;
  private long lastSyncTime = System.currentTimeMillis();

  public TickCounter sync() {
    long currentTickId = getCurrentTickId();
    long currentTickDurationPassed = getCurrentTickDurationPassed();

    // If closer to the next tick than the last tick, pull the tick id forward.
    if (currentTickDurationPassed >= TICK_MS / 2) {
      currentTickId++;
    }

    lastSyncTickId = currentTickId;
    lastSyncTime = System.currentTimeMillis();
    return this;
  }

  public long getCurrentTickId() {
    return (System.currentTimeMillis() - lastSyncTime) / TICK_MS + lastSyncTickId;
  }

  public long getCurrentTickDurationPassed() {
    return (System.currentTimeMillis() - lastSyncTime) % TICK_MS;
  }
}

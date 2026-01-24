package com.osmbtoolkit.job;

public abstract class Job<T extends JobLoopScript> {
  protected final T script;

  private final JobConfig config;

  public Job(T script) {
    this(script, new JobConfig.Builder().build());
  }
  public Job(T script, JobConfig config) {
    this.config = config;
    this.script = script;
  }

  /**
   * Potential requirements to think about:
   * <ul>
   *   <li>Is the player in the correct location/region?</li>
   *   <li>Does the player have the right items?</li>
   *   <li>Are there other higher-priority jobs that need to execute first?</li>
   * </ul>
   */
  public abstract boolean canExecute();

  public abstract boolean execute();

  public JobConfig getConfig() {
    return config;
  }
}

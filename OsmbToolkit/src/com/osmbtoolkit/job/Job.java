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

  @Override
  public String toString() {
    String name = getClass().getSimpleName();
    if (name.endsWith("Job")) name = name.substring(0, name.length() - "Job".length());
    if (name.endsWith("Task")) name = name.substring(0, name.length() - "Task".length());
    name = name.replaceAll("(?<!^)([A-Z])", " $1");
    name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    return name.trim();
  }
}

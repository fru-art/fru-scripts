package com.osmbtoolkit.job;

public class JobConfig {
  public final boolean canBreakScript;
  public final boolean canRetry;
  public final int retryLimit;

  private JobConfig(
      boolean canBreakScript,
      boolean canRetry,
      int retryLimit) {
    this.canBreakScript = canBreakScript;
    this.canRetry = canRetry;
    this.retryLimit = retryLimit;
  }

  public static class Builder {
    private boolean canBreakScript = false;
    private boolean canRetry = false;
    private int retryLimit = 1;

    public JobConfig build() {
      return new JobConfig(canBreakScript, canRetry, retryLimit);
    }

    /**
     * @param canBreakScript Script will exit if the job fails. Job will retry first if configured to do so.
     */
    public Builder canBreakScript(boolean canBreakScript) {
      this.canBreakScript = canBreakScript;
      return this;
    }

    public Builder canRetry(boolean canRetry) {
      this.canRetry = canRetry;
      return this;
    }

    /**
     * @param retryLimit Number of times to retry upon failure. Does nothing unless canRetry=true.
     */
    public Builder retryLimit(int retryLimit) {
      this.retryLimit = retryLimit;
      return this;
    }
  }
}

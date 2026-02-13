package com.osmbtoolkit.job;

import com.osmb.api.script.Script;

import java.util.List;
import java.util.Optional;

public abstract class JobLoopScript extends Script {
  private static final int JOB_DEBOUNCE = 1_200;

  private Optional<Job> currentJobDebounced = Optional.empty();
  private long currentJobUpdated = System.currentTimeMillis();
  private JobSequence jobSequence;

  public JobLoopScript(Object scriptCore) {
    super(scriptCore);
  }

  @Override
  public int poll() {
    if (jobSequence == null) {
      // Initialize on first poll because it's common for jobs to require the script to already be initialized or
      // options to be set before the jobs are created
      this.jobSequence = new JobSequence(this, getJobs());
      this.jobSequence.addJobChangeListener(this::onJobChange);
    }
    jobSequence.execute();
    return 0;
  }

  public Optional<Job> getCurrentJob() {
    if (jobSequence == null) return Optional.empty();
    return jobSequence.getCurrentJob();
  }

  /**
   * @return the last known job until a certain interval has passed.
   */
  public Optional<Job> getCurrentJobDebounced() {
    Optional<Job> currentJob = getCurrentJob();
    long now = System.currentTimeMillis();

    if (currentJob.isEmpty()) {
      if (currentJobDebounced.isPresent() && now - currentJobUpdated < JOB_DEBOUNCE) {
        return currentJobDebounced;
      }
      currentJobDebounced = Optional.empty();
      currentJobUpdated = now;
      return Optional.empty();
    }

    currentJobDebounced = currentJob;
    currentJobUpdated = now;
    return currentJob;
  }

  public void onJobChange(Optional<Job> job) {}

  public abstract List<Job> getJobs();
}

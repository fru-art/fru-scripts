package com.osmbtoolkit.job;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JobSequence {
  private final List<Job> jobs;
  private final JobLoopScript script;

  private Job currentJob;

  public JobSequence(JobLoopScript script, List<Job> jobs) {
    this.jobs = jobs.stream().filter(Objects::nonNull).toList();
    this.script = script;
  }

  public boolean execute() {
    for (Job job : jobs) {
      JobConfig jobConfig = job.getConfig();
      boolean canExecute = job.canExecute();

      if (!canExecute) {
        script.log(getClass(), "Skipping execution for " + job.getClass().getSimpleName());
        continue;
      }

      currentJob = job;
      Integer retryCount = null;
      boolean executed;

      do {
        script.log(getClass(),
          (retryCount == null ? "Starting" : "Restarting") + " execution for " + job.getClass().getSimpleName());
        executed = job.execute();
        retryCount = retryCount == null ? 0 : retryCount + 1;
      } while (!executed && jobConfig.canRetry && retryCount < jobConfig.retryLimit);

      currentJob = null;
      if (!executed && jobConfig.canBreakScript) {
        script.stop();
        return false;
      }
    }

    return true;
  }

  public Optional<Job> getCurrentJob() {
    return Optional.ofNullable(currentJob);
  }
}

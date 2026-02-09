package com.osmbtoolkit.job;

import java.util.*;
import java.util.function.Consumer;

public class JobSequence {
  private final List<Job> jobs;
  private final Set<Consumer<Optional<Job>>> jobChangeListeners = new HashSet<>();
  private final JobLoopScript script;

  private Job currentJob;

  public JobSequence(JobLoopScript script, List<Job> jobs) {
    this.jobs = jobs.stream().filter(Objects::nonNull).toList();
    this.script = script;
  }

  public void addJobChangeListener(Consumer<Optional<Job>> listener) {
    this.jobChangeListeners.add(listener);
  }

  public boolean execute() {
    for (Job job : jobs) {
      if (script.stopped()) return false;

      JobConfig jobConfig = job.getConfig();
      boolean canExecute = job.canExecute();

      if (!canExecute) {
        script.log(getClass(), "Skipping execution for " + job.getClass().getSimpleName());
        continue;
      }

      currentJob = job;
      for (Consumer<Optional<Job>> jobChangeListener : jobChangeListeners) {
        jobChangeListener.accept(Optional.ofNullable(currentJob));
      }
      Integer retryCount = null;
      boolean executed;

      do {
        script.log(getClass(),
          (retryCount == null ? "Starting" : "Restarting") + " execution for " + job.getClass().getSimpleName());
        script.pollFramesUntil(() -> false, 0); // Clear draw queue and execute tasks
        executed = job.execute();
        retryCount = retryCount == null ? 0 : retryCount + 1;
      } while (!executed && jobConfig.canRetry && retryCount < jobConfig.retryLimit);

      currentJob = null;
      for (Consumer<Optional<Job>> jobChangeListener : jobChangeListeners) {
        jobChangeListener.accept(Optional.empty());
      }
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

  public void removeJobChangeListener(Consumer<Optional<Job>> listener) {
    this.jobChangeListeners.remove(listener);
  }
}

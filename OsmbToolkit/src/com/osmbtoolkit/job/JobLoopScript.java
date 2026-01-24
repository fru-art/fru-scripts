package com.osmbtoolkit.job;

import com.osmb.api.script.Script;

import java.util.List;
import java.util.Optional;

public abstract class JobLoopScript extends Script {
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
    }
    jobSequence.execute();
    return 0;
  }

  abstract protected List<Job> getJobs();

  protected Optional<Job> getCurrentJob() {
    return jobSequence.getCurrentJob();
  }
}

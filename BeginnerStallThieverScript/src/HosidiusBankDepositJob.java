import com.osmbtoolkit.job.impl.BankDepositJob;

import java.util.Set;

public class HosidiusBankDepositJob extends BankDepositJob {
  private final BeginnerStallThieverScript script;

  public HosidiusBankDepositJob(BeginnerStallThieverScript script) {
    super(script);
    this.script = script;
  }

  @Override
  public boolean execute() {
    if (!script.hosidius.passHouse(false)) {
      script.log(getClass(), "Failed to exit Hosidius house");
    }

    return super.execute();
  }
}

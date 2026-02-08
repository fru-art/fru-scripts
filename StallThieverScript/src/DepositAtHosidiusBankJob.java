import com.osmbtoolkit.job.impl.DepositAtBankJob;

public class DepositAtHosidiusBankJob extends DepositAtBankJob {
  private final StallThieverScript script;

  public DepositAtHosidiusBankJob(StallThieverScript script) {
    super(script);
    this.script = script;
  }

  @Override
  public boolean execute() {
    if (!script.hosidiusHouse.passThrough(false)) {
      script.log(getClass(), "Failed to exit Hosidius house");
    }
    return super.execute();
  }

  @Override
  public String toString() {
    return "Deposit at Hosidius bank";
  }
}

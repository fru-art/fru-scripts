import com.osmbtoolkit.job.impl.BankDepositJob;

public class HosidiusBankDepositJob extends BankDepositJob {
  private final StallThieverScript script;

  public HosidiusBankDepositJob(StallThieverScript script) {
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

  @Override
  public String toString() {
    return "Deposit at Hosidius bank";
  }
}

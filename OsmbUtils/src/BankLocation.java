import com.osmb.api.location.position.types.WorldPosition;

import java.util.Set;

public class BankLocation {
  public final WorldPosition approximatePosition;
  public final Set<Integer> supportedRegions;

  public BankLocation(Set<Integer> supportedRegions, WorldPosition approximatePosition) {
    this.supportedRegions = Set.copyOf(supportedRegions);
    this.approximatePosition = approximatePosition;
  }
}

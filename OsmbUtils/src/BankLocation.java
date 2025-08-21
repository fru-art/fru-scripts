import com.osmb.api.location.position.types.WorldPosition;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BankLocation {
  public final WorldPosition approximatePosition;
  public final Set<Integer> supportedRegions;

  public BankLocation(Set<Integer> supportedRegions, WorldPosition approximatePosition) {
    this.supportedRegions = Set.copyOf(supportedRegions);
    this.approximatePosition = approximatePosition;
  }

  public static Map<Integer, BankLocation> getRegionToBankMap(Set<BankLocation> bankLocations) {
    return bankLocations.stream()
      .flatMap(bankLocation -> bankLocation.supportedRegions.stream()
        .map(regionId -> Map.entry(regionId, bankLocation)))
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (prevValue, value) -> value
      ));
  }
}

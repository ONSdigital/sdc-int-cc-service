package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@JsonPropertyOrder({"uprn", "formattedAddress", "welshFormattedAddress"})
public class AddressDTO {
  private String uprn;
  private String region;
  private String addressType;

  @LoggingScope(scope = Scope.MASK)
  private String estabType;

  @LoggingScope(scope = Scope.MASK)
  private String estabDescription;

  @LoggingScope(scope = Scope.MASK)
  private String formattedAddress;

  @LoggingScope(scope = Scope.MASK)
  private String welshFormattedAddress;
}

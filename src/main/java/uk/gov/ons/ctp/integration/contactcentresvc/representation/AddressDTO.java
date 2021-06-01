package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import lombok.Data;

@Data
@JsonPropertyOrder({"uprn", "formattedAddress", "welshFormattedAddress"})
public class AddressDTO {

  private String uprn;

  private String region;

  private String addressType;

  @LoggingScope(scope = Scope.SKIP)
  private String estabType;

  @LoggingScope(scope = Scope.SKIP)
  private String estabDescription;

  @LoggingScope(scope = Scope.SKIP)
  private String formattedAddress;

  @LoggingScope(scope = Scope.SKIP)
  private String welshFormattedAddress;
}

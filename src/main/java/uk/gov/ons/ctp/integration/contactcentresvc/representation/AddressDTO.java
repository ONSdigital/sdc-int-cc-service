package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"uprn", "formattedAddress", "welshFormattedAddress"})
public class AddressDTO {
  private String uprn;
  private String region;
  private String addressType;
  private String estabType;
  private String estabDescription;
  private String formattedAddress;
  private String welshFormattedAddress;
}

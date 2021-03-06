package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * An Address representation
 *
 * @author philwhiles
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressIndexAddressCensusDTO {
  private String countryCode;

  private String addressType;

  private String estabType;
}

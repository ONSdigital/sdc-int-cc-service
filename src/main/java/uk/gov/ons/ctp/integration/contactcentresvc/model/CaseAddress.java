package uk.gov.ons.ctp.integration.contactcentresvc.model;

import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Embeddable
public class CaseAddress {
  private String uprn;

  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;

  private String region; // E, W or N
  private String estabType;
  private String organisationName;

  private String latitude;
  private String longitude;
  private String estabUprn;
  private String addressType;
  private String addressLevel;
}

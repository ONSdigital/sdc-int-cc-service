package uk.gov.ons.ctp.integration.contactcentresvc.model;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Region;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class CaseAddress {
  private String uprn;

  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;

  @Enumerated(EnumType.STRING)
  private Region region;
}

package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

/**
 * The response object when contact centre requests case details
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDTO {

  private UUID id;

  private String caseRef;

  private String caseType;

  private String addressType;

  private EstabType estabType;

  private String estabDescription;

  private List<DeliveryChannel> allowedDeliveryChannels;

  private Date createdDateTime;

  private Date lastUpdated;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String region;

  private String postcode;

  @Size(max = 60)
  private String ceOrgName;

  private UniquePropertyReferenceNumber uprn;

  private UniquePropertyReferenceNumber estabUprn;

  private List<CaseEventDTO> caseEvents;

  private boolean secureEstablishment;

  private boolean handDelivery;

  @JsonIgnore
  public boolean isHandDelivery() {
    return handDelivery;
  }
}

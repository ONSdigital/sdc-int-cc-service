package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

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

  @LoggingScope(scope = Scope.HASH)
  private String caseRef;

  private List<DeliveryChannel> allowedDeliveryChannels;

  private String addressLine1;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine2;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine3;

  @LoggingScope(scope = Scope.MASK)
  private String townName;

  private String region;

  private String postcode;

  private UniquePropertyReferenceNumber uprn;

  private List<CaseEventDTO> caseEvents;
}

package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAddressDTO {

  private UniquePropertyReferenceNumber uprn;

  private String addressLine1;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine2;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine3;

  @LoggingScope(scope = Scope.MASK)
  private String townName;

  private String postcode;

  private String region;
}

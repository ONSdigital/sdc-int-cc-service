package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.UUID;

import lombok.Data;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
public class CaseSummaryDTO {

  private UUID id;

  @LoggingScope(scope = Scope.HASH)
  private String caseRef;

  private String surveyName;
  
  private String surveyType;
}

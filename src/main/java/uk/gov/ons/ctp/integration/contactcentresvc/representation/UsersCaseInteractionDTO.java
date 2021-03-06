package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersCaseInteractionDTO {

  private UUID caseId;

  private SurveyType surveyType;

  @LoggingScope(scope = Scope.HASH)
  private String caseRef;

  private CaseInteractionType interaction;

  private CaseSubInteractionType subInteraction;

  private String note;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  private LocalDateTime createdDateTime;
}

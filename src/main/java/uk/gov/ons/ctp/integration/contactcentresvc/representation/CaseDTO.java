package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.SurveyType;
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

  private UUID collectionExerciseId;

  private UUID surveyId;
  private SurveyType surveyType;

  @LoggingScope(scope = Scope.HASH)
  private String caseRef;

  private Map<String, Object> sample;

  private Map<String, String> sampleSensitive;

  private List<CaseInteractionDTO> interactions;

  // Set to true if this can be lauched. ie. the current date is within a wave
  private boolean canLaunch;
}

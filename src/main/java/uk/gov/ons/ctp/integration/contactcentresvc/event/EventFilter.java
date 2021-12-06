package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@Slf4j
@Component
public class EventFilter {

  private final AppConfig appConfig;
  private final SurveyRepository surveyRepository;
  private final CollectionExerciseRepository collectionExerciseRepository;

  public EventFilter(
      AppConfig appConfig,
      SurveyRepository surveyRepository,
      CollectionExerciseRepository collectionExerciseRepository) {
    this.appConfig = appConfig;
    this.surveyRepository = surveyRepository;
    this.collectionExerciseRepository = collectionExerciseRepository;
  }

  public boolean isValidEvent(String surveyId, String collexId, String caseId, String messageId)
      throws CTPException {

    log.info("Entering acceptCaseEvent {}, {}", kv("messageId", messageId), kv("caseId", caseId));

    Survey survey = findSurvey(surveyId, messageId, caseId);

    return isAcceptedSurveyType(survey, messageId, caseId)
        && isKnownCollectionExercise(collexId, messageId, caseId);
  }

  private Survey findSurvey(String surveyId, String messageId, String caseId) throws CTPException {
    Survey survey = surveyRepository.findById(UUID.fromString(surveyId)).orElse(null);
    if (survey == null) {
      log.warn("Survey unknown - NAKing message", kv("messageId", messageId), kv("caseId", caseId));
      throw new CTPException(CTPException.Fault.VALIDATION_FAILED, "Survey unknown");
    }
    return survey;
  }

  private boolean isAcceptedSurveyType(Survey survey, String messageId, String caseId) {
    if (survey != null) {
      Set<String> surveysTypes = appConfig.getSurveys();
      for (String surveyType : surveysTypes) {
        if (survey.getSampleDefinitionUrl().endsWith(surveyType + ".json")) {
          return true;
        }
      }
    }
    log.warn(
        "Survey is not an accepted survey type - discarding message",
        kv("messageId", messageId),
        kv("caseId", caseId));
    return false;
  }

  private boolean isKnownCollectionExercise(String collexId, String messageId, String caseId)
      throws CTPException {
    CollectionExercise collex =
        collectionExerciseRepository.findById(UUID.fromString(collexId)).orElse(null);
    if (collex == null) {
      log.warn(
          "CollectionExercise unknown - NAKing message",
          kv("messageId", messageId),
          kv("caseId", caseId));
      throw new CTPException(CTPException.Fault.VALIDATION_FAILED, "CollectionExercise unknown");
    }
    return true;
  }
}

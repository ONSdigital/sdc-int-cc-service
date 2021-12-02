package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@Slf4j
@Component
public class EventFilter {

  private AppConfig appConfig;
  private SurveyRepository surveyRepository;
  private CollectionExerciseRepository collectionExerciseRepository;

  public EventFilter(
      AppConfig appConfig,
      SurveyRepository surveyRepository,
      CollectionExerciseRepository collectionExerciseRepository) {
    this.appConfig = appConfig;
    this.surveyRepository = surveyRepository;
    this.collectionExerciseRepository = collectionExerciseRepository;
  }

  public boolean isValidEvent(String surveyId, String collexId, String caseId, String messageId) {

    log.info("Entering acceptCaseEvent {}, {}", kv("messageId", messageId), kv("caseId", caseId));

    Survey survey = findSurvey(surveyId);
    boolean acceptedSurveyType = isAcceptedSurveyType(survey);
    boolean valid = acceptedSurveyType && isKnownCollectionExercise(collexId);

    if (valid) {
      return true;
    } else {
      logDiscardMessage(survey, messageId, acceptedSurveyType, caseId);
      return false;
    }
  }

  private Survey findSurvey(String surveyId) {
    return surveyRepository.getById(UUID.fromString(surveyId));
  }

  private boolean isAcceptedSurveyType(Survey survey) {
    if (survey != null) {
      Set<String> surveysTypes = appConfig.getSurveys();
      for (String surveyType : surveysTypes) {
        if (survey.getSampleDefinitionUrl().endsWith(surveyType + ".json")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isKnownCollectionExercise(String collexId) {
    CollectionExercise collEx = collectionExerciseRepository.getById(UUID.fromString(collexId));
    return collEx != null;
  }

  /*
   * TODO - should we NAK the event/throw exception if we do not recognise
   * the survey and allow the exception manager quarantine the event or
   * allow to go to DLQ?
   */
  private void logDiscardMessage(
      Survey survey, String messageId, boolean acceptedSurveyType, String caseId) {

    String msg = null;
    if (survey == null) {
      msg = "Case Survey unknown - discarding message";
    } else if (!acceptedSurveyType) {
      msg = "Survey is not an accepted survey type - discarding message";
    } else {
      msg = "Case CollectionExercise unknown - discarding message";
    }
    log.warn(msg, kv("messageId", messageId), kv("caseId", caseId));
  }
}

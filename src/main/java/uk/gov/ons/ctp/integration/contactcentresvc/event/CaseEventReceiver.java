package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of inbound queue.
 */
@Slf4j
@MessageEndpoint
public class CaseEventReceiver {
  private CaseRepository caseRepo;
  private SurveyRepository surveyRepo;
  private CollectionExerciseRepository collExRepo;
  private MapperFacade mapper;

  public CaseEventReceiver(
      CaseRepository caseRepo,
      SurveyRepository surveyRepo,
      CollectionExerciseRepository collExRepo,
      MapperFacade mapper) {
    this.caseRepo = caseRepo;
    this.surveyRepo = surveyRepo;
    this.collExRepo = collExRepo;
    this.mapper = mapper;
  }

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptEvent(CaseEvent caseEvent) {

    CaseUpdate caseUpdate = caseEvent.getPayload().getCaseUpdate();
    UUID caseMessageId = caseEvent.getHeader().getMessageId();

    log.info(
        "Entering acceptCaseEvent {}, {}",
        kv("messageId", caseMessageId),
        kv("caseId", caseUpdate.getCaseId()));

    Survey survey = findSurvey(caseUpdate);
    boolean social = isSocial(survey);
    boolean valid = social && isKnownCollectionExercise(caseUpdate);

    if (valid) {
      try {
        Case caze = map(caseUpdate);
        caseRepo.save(caze);

        log.info(
            "Successful saved Case to database {}, {}",
            kv("messageId", caseMessageId),
            kv("caseId", caseUpdate.getCaseId()));
      } catch (Exception e) {
        log.error("Case Event processing failed", kv("messageId", caseMessageId), e);
        throw e;
      }
    } else {
      logDiscardMessage(survey, social, caseEvent);
    }
  }

  private Survey findSurvey(CaseUpdate caseUpdate) {
    return surveyRepo.getById(UUID.fromString(caseUpdate.getSurveyId()));
  }

  private boolean isSocial(Survey survey) {
    return survey != null && survey.getSampleDefinitionUrl().endsWith("social.json");
  }

  private boolean isKnownCollectionExercise(CaseUpdate caseUpdate) {
    CollectionExercise collEx =
        collExRepo.getById(UUID.fromString(caseUpdate.getCollectionExerciseId()));
    return collEx != null;
  }

  /*
   * TODO - should we NAK the event/throw exception if we do not recognise
   * the survey and allow the exception manager quarantine the event or
   * allow to go to DLQ?
   */
  private void logDiscardMessage(Survey survey, boolean social, CaseEvent event) {
    CaseUpdate caseUpdate = event.getPayload().getCaseUpdate();
    UUID caseMessageId = event.getHeader().getMessageId();

    String msg = null;
    if (survey == null) {
      msg = "Case Survey unknown - discarding case";
    } else if (!social) {
      msg = "Survey is not a social survey - discarding case";
    } else {
      msg = "Case CollectionExercise unknown - discarding case";
    }
    log.warn(msg, kv("messageId", caseMessageId), kv("caseId", caseUpdate.getCaseId()));
  }

  private Case map(CaseUpdate caseUpdate) {
    Case caze = mapper.map(caseUpdate, Case.class);

    // TODO: remove this later. this is temporary code .
    // Hard code this until elements added to CaseUpdate
    // these lines should then be removed.
    if (caze.getCaseRef() == null) {
      String base = Integer.toString(10_000_000 + new Random().nextInt(9_999_999));
      try {
        String caseRef = base + new LuhnCheckDigit().calculate(base);
        caze.setCaseRef(caseRef);
        caze.setCreatedAt(LocalDateTime.now());
        caze.setLastUpdatedAt(LocalDateTime.now());

      } catch (CheckDigitException e) {
        throw new RuntimeException(e);
      }
    }
    // -----

    return caze;
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
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
  private AcceptableEventFilter acceptableEventFilter;

  public CaseEventReceiver(
      CaseRepository caseRepo,
      SurveyRepository surveyRepo,
      CollectionExerciseRepository collExRepo,
      MapperFacade mapper,
      AcceptableEventFilter acceptableEventFilter) {
    this.caseRepo = caseRepo;
    this.surveyRepo = surveyRepo;
    this.collExRepo = collExRepo;
    this.mapper = mapper;
    this.acceptableEventFilter = acceptableEventFilter;
  }

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  @Transactional
  public void acceptEvent(CaseEvent caseEvent) {

    CaseUpdate caseUpdate = caseEvent.getPayload().getCaseUpdate();
    UUID caseMessageId = caseEvent.getHeader().getMessageId();

    log.info(
        "Entering acceptCaseEvent {}, {}",
        kv("messageId", caseMessageId),
        kv("caseId", caseUpdate.getCaseId()));

    if (acceptableEventFilter.filterEvent(
        caseUpdate.getSurveyId(),
        caseUpdate.getCollectionExerciseId(),
        caseUpdate.getCaseId(),
        caseMessageId.toString())) {
      try {
        Case caze = mapper.map(caseUpdate, Case.class);
        caseRepo.save(caze);

        log.info(
            "Successful saved Case to database {}, {}",
            kv("messageId", caseMessageId),
            kv("caseId", caseUpdate.getCaseId()));
      } catch (Exception e) {
        log.error("Case Event processing failed", kv("messageId", caseMessageId), e);
        throw e;
      }
    }
  }
}

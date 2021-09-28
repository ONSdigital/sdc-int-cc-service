package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of inbound queue.
 */
@Slf4j
@MessageEndpoint
public class CaseEventReceiver {

  @Autowired CaseRepository caseRepo;

  private MapperFacade mapper = new CCSvcBeanMapper();

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) {

    CollectionCase collectionCase = caseEvent.getPayload().getCollectionCase();
    String caseTransactionId = caseEvent.getHeader().getMessageId();

    log.info(
        "Entering acceptCaseEvent {}, {}",
        kv("transactionId", caseTransactionId),
        kv("caseId", collectionCase.getId()));

    Case caze = map(collectionCase);
    caseRepo.save(caze);

    log.info(
        "Successful saved Case to database {}, {}",
        kv("transactionId", caseTransactionId),
        kv("caseId", collectionCase.getId()));
  }

  private Case map(CollectionCase collectionCase) {
    Case caze = mapper.map(collectionCase, Case.class);
    return caze;
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
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
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) throws CTPException {

    CollectionCase collectionCase = caseEvent.getPayload().getCollectionCase();
    String caseTransactionId = caseEvent.getEvent().getTransactionId();

    log.info(
        "Entering acceptCaseEvent",
        kv("transactionId", caseTransactionId),
        kv("caseId", collectionCase.getId()));

    Case caze = map(collectionCase);
    caseRepo.save(caze);
  }

  private Case map(CollectionCase collectionCase) {
    Case caze = mapper.map(collectionCase, Case.class);
    return caze;
  }
}

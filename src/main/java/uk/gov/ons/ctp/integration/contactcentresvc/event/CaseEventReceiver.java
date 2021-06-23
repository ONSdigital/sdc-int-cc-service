package uk.gov.ons.ctp.integration.contactcentresvc.event;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import lombok.Data;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of inbound queue.
 */
@Data
@MessageEndpoint
public class CaseEventReceiver {
  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiver.class);

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

    log.with("transactionId", caseTransactionId)
        .with("caseId", collectionCase.getId())
        .info("Entering acceptCaseEvent");

    // TODO processing code
  }
}

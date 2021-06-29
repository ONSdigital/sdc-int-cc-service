package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static net.logstash.logback.argument.StructuredArguments.kv;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of inbound queue.
 */
@Slf4j
@Data
@MessageEndpoint
public class CaseEventReceiver {

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

    log.info("Entering acceptCaseEvent",
        kv("transactionId", caseTransactionId),
        kv("caseId", collectionCase.getId()));

    // TODO processing code
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.OffsetDateTime;
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
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of inbound queue.
 */
@Slf4j
@MessageEndpoint
public class CaseEventReceiver {
  private CaseRepository caseRepo;
  private MapperFacade mapper;

  public CaseEventReceiver(CaseRepository caseRepo, MapperFacade mapper) {
    this.caseRepo = caseRepo;
    this.mapper = mapper;
  }

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) {

    CaseUpdate caseUpdate = caseEvent.getPayload().getCaseUpdate();
    UUID caseMessageId = caseEvent.getHeader().getMessageId();

    log.info(
        "Entering acceptCaseEvent {}, {}",
        kv("messageId", caseMessageId),
        kv("caseId", caseUpdate.getCaseId()));

    Case caze = map(caseUpdate);
    caseRepo.save(caze);

    log.info(
        "Successful saved Case to database {}, {}",
        kv("messageId", caseMessageId),
        kv("caseId", caseUpdate.getCaseId()));
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
        caze.setCreatedAt(OffsetDateTime.now());
        caze.setLastUpdatedAt(OffsetDateTime.now());

      } catch (CheckDigitException e) {
        throw new RuntimeException(e);
      }
    }
    // -----

    return caze;
  }
}

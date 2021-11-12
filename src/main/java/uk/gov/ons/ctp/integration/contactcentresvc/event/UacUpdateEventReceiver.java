package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseResponse;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseResponseRepository;

/** Service implementation responsible for receipt of UacUpdate Events. */
@Slf4j
@MessageEndpoint
public class UacUpdateEventReceiver {

  private CaseResponseRepository caseResponseRepository;
  private CaseRepository caseRepository;
  private MapperFacade mapper;

  public UacUpdateEventReceiver(
      CaseResponseRepository repo, CaseRepository caseRepo, MapperFacade mapper) {
    this.caseResponseRepository = repo;
    this.caseRepository = caseRepo;
    this.mapper = mapper;
  }

  /**
   * Message end point for events from Response Management.
   *
   * @param uacEvent UacEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUacEvent")
  @Transactional
  public void acceptEvent(UacEvent uacEvent) {

    UacUpdate uac = uacEvent.getPayload().getUacUpdate();
    String uacMessageId = uacEvent.getHeader().getMessageId().toString();

    log.info(
        "Entering acceptUACEvent", kv("messageId", uacMessageId), kv("caseId", uac.getCaseId()));

    Optional<Case> caseOptional = caseRepository.findById(UUID.fromString(uac.getCaseId()));

    if (caseOptional.isPresent()) {
      CaseResponse response = mapper.map(uac, CaseResponse.class);

      System.out.println(response.getCaseId());
      try {
        caseResponseRepository.save(response);
      } catch (Exception e) {
        log.error("Uac Event processing failed", kv("messageId", uacMessageId), e);
        throw e;
      }
    } else {
      log.info(
          "Case not found, discarding message",
          kv("messageId", uacMessageId),
          kv("caseId", uac.getCaseId()));
    }
  }
}

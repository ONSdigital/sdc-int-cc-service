package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UacRepository;

/** Service implementation responsible for receipt of UacUpdate Events. */
@Slf4j
@MessageEndpoint
public class UacUpdateEventReceiver {

  private UacRepository uacRepository;
  private CaseRepository caseRepository;
  private MapperFacade mapper;
  private EventFilter eventFilter;

  public UacUpdateEventReceiver(
      UacRepository repo, CaseRepository caseRepo, MapperFacade mapper, EventFilter eventFilter) {
    this.uacRepository = repo;
    this.caseRepository = caseRepo;
    this.mapper = mapper;
    this.eventFilter = eventFilter;
  }

  /**
   * Message end point for events from Response Management.
   *
   * @param uacEvent UacEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUacEvent")
  @Transactional
  public void acceptEvent(UacEvent uacEvent) throws CTPException {

    UacUpdate uacUpdate = uacEvent.getPayload().getUacUpdate();
    String uacMessageId = uacEvent.getHeader().getMessageId().toString();

    log.info(
        "Entering acceptUACEvent",
        kv("messageId", uacMessageId),
        kv("caseId", uacUpdate.getCaseId()));

    if (eventFilter.isValidEvent(
        uacUpdate.getSurveyId(),
        uacUpdate.getCollectionExerciseId(),
        uacUpdate.getCaseId(),
        uacMessageId)) {
      Uac uac = mapper.map(uacUpdate, Uac.class);
      Optional<Case> caseOptional = caseRepository.findById(UUID.fromString(uacUpdate.getCaseId()));
      if (caseOptional.isPresent()) {
        try {
          uacRepository.saveAndFlush(uac);
        } catch (Exception e) {
          log.error("UAC Event processing failed", kv("messageId", uacMessageId), e);
          throw e;
        }
      } else {
        log.info(
            "Case not found, creating skeleton case",
            kv("messageId", uacMessageId),
            kv("caseId", uacUpdate.getCaseId()));
        Case collectionCase = createSkeletonCase(uac);
        try {
          caseRepository.saveAndFlush(collectionCase);
          uacRepository.saveAndFlush(uac);
        } catch (Exception e) {
          log.error("UAC Event processing failed", kv("messageId", uacMessageId), e);
          throw e;
        }
      }
    }
  }

  // placeholder values for fields that cannot be NULL
  private Case createSkeletonCase(Uac uac) {
    Case collectionCase = new Case();
    collectionCase.setId(uac.getCaseId());
    collectionCase.setCcStatus(CCStatus.PENDING);
    collectionCase.setCaseRef("");
    collectionCase.setLastUpdatedAt(LocalDateTime.parse("9999-01-01T00:00:00.000"));
    collectionCase.setCreatedAt(LocalDateTime.parse("9999-01-01T00:00:00.000"));
    collectionCase.setCollectionExercise(
        CollectionExercise.builder().id(uac.getCollectionExerciseId()).build());
    collectionCase.setAddress(
        CaseAddress.builder()
            .uprn("")
            .addressLine1("")
            .townName("")
            .postcode("")
            .region(Region.E)
            .build());

    return collectionCase;
  }
}

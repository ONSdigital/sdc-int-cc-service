package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

@Slf4j
@Service
public class InteractionService {

  @Autowired private CaseInteractionRepository caseRepository;
  @Autowired private UserRepository userRepository;

  @Autowired private CCSvcBeanMapper mapper;

  public ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionRequestDTO interaction)
      throws CTPException {
    CaseInteraction userInteraction = mapper.map(interaction, CaseInteraction.class);
    userInteraction.setCaseId(caseId);
    userInteraction.setCreatedDateTime(LocalDateTime.now());

    String userName = UserIdentityContext.get();
    User user =
        userRepository
            .findByName(userName)
            .orElseThrow(
                () -> new CTPException(Fault.SYSTEM_ERROR, "User in context cannot be found"));

    userInteraction.setCcuser(user);

    log.debug("Saving interaction for case", kv("caseId", caseId));

    try {
      caseRepository.saveAndFlush(userInteraction);
    } catch (Exception e) {
      log.error("Failed to save case interaction", kv("caseId", caseId), e);
      throw e;
    }

    log.debug("Returning response for case", kv("caseId", caseId));
    // Build response
    return ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();
  }
}

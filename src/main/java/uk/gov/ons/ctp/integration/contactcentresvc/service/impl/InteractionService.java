package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

@Slf4j
@Service
public class InteractionService {

  @Autowired private CaseInteractionRepository interactionRepository;
  @Autowired private RBACService rbacService;
  @Autowired private AppConfig appConfig;

  @Autowired private CCSvcBeanMapper mapper;

  public ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionRequestDTO interaction)
      throws CTPException {
    CaseInteraction caseInteraction = mapper.map(interaction, CaseInteraction.class);
    caseInteraction.setCaseId(caseId);
    caseInteraction.setCreatedDateTime(LocalDateTime.now());

    User user = null;

    if (rbacService.userActingAsAllowedDummy() || true) {
      user = appConfig.getDummyUserConfig().getDummyUser();
    } else {
      user = rbacService.loadUser();
    }

    caseInteraction.setCcuser(user);

    log.debug("Saving interaction for case", kv("caseId", caseId));

    try {
      interactionRepository.saveAndFlush(caseInteraction);
    } catch (Exception e) {
      log.error("Failed to save case interaction", kv("caseId", caseId), e);
      throw e;
    }

    log.debug("Returning response for case", kv("caseId", caseId));
    // Build response
    return ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();
  }
}

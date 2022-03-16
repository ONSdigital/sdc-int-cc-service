package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;

@Slf4j
@Service
public class InteractionService {

  @Autowired private CaseInteractionRepository interactionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RBACService rbacService;
  @Autowired private AppConfig appConfig;
  @Autowired private CCSvcBeanMapper mapper;

  public ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionRequestDTO interaction)
      throws CTPException {
    CaseInteraction caseInteraction = mapper.map(interaction, CaseInteraction.class);
    caseInteraction.setCaze(Case.builder().id(caseId).build());
    caseInteraction.setCreatedDateTime(LocalDateTime.now());

    User user = null;

    if (rbacService.userActingAsAllowedDummy()) {
      user = appConfig.getDummyUserConfig().getDummyUser();
    } else {
      user = rbacService.loadUser();
    }

    caseInteraction.setCcuser(user);

    if (!hasValidSubtype(interaction)) {
      String message = "Wrong subtype supplied for interaction";
      log.warn(message, kv("caseId", caseId));
      throw new CTPException(Fault.VALIDATION_FAILED, message);
    }

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

  public List<UsersCaseInteractionDTO> getAllCaseInteractionsForUser(String userIdentity)
      throws CTPException {

    User user =
        userRepository
            .findByIdentity(userIdentity)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    List<CaseInteraction> caseInteractions = interactionRepository.findAllByCcuserId(user.getId());

    List<UsersCaseInteractionDTO> response =
        mapper.mapAsList(caseInteractions, UsersCaseInteractionDTO.class);

    return response.stream()
        .sorted(Comparator.comparing(UsersCaseInteractionDTO::getCreatedDateTime).reversed())
        .toList();
  }

  private boolean hasValidSubtype(CaseInteractionRequestDTO dto) {
    return dto.getType().isValidSubType(dto.getSubtype());
  }
}

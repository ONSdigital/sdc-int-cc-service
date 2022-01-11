package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.InteractionService;

@Slf4j
@Service
public class InteractionServiceImpl implements InteractionService {

  @Autowired private CaseInteractionRepository repository;

  @Autowired private CCSvcBeanMapper mapper;

  public ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionDTO interaction) {
    CaseInteraction caseInteraction = mapper.map(interaction, CaseInteraction.class);
    caseInteraction.setCaseId(caseId);
    caseInteraction.setCreatedDateTime(LocalDateTime.now());

    //Will eventually be replaced wit actual UserId
    User user = User.builder().id(UUID.fromString("382a8474-479c-11ec-a052-4c3275913db5")).build();
    caseInteraction.setCcuser(user);

    log.debug("Saving interaction for case", kv("caseId", caseId));

    try {
      repository.saveAndFlush(caseInteraction);
    } catch (Exception e) {
      log.error("Failed to save case interaction", kv("caseId", caseId), e);
      throw e;
    }

    log.debug("Returning response for case", kv("caseId", caseId));
    // Build response
    return ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();
  }
}

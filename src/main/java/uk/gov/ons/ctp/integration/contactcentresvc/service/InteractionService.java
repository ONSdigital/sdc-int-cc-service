package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.UUID;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

public interface InteractionService {

  ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionRequestDTO interaction);
}

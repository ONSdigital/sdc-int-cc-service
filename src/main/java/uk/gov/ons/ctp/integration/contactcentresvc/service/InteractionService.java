package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.UUID;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

public interface InteractionService {

  ResponseDTO saveCaseInteraction(UUID caseId, CaseInteractionDTO interaction);
}

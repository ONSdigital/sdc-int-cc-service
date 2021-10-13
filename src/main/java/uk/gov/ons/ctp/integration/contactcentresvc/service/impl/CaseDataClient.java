package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@Slf4j
@Service
public class CaseDataClient {
  private CaseRepository caseRepo;

  public CaseDataClient(CaseRepository caseRepo) {
    this.caseRepo = caseRepo;
  }

  public Case getCaseById(UUID caseId) throws CTPException {
    log.debug("Find case details by ID", kv("caseId", caseId));

    Case caze =
        caseRepo
            .findById(caseId)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND, "Could not find case for ID: " + caseId));

    log.debug("Found case details for case ID", kv("caseId", caseId));
    return caze;
  }

  public List<Case> getCaseByUprn(Long uprn) throws CTPException {
    log.debug("Find case details by Uprn", kv("uprn", uprn));

    List<Case> cases = caseRepo.findByAddressUprn(uprn.toString());

    log.debug("Found {} case details by Uprn", cases.size(), kv("uprn", uprn));
    return cases;
  }

  public Case getCaseByCaseRef(Long caseReference) throws CTPException {
    log.debug("Find case details by case reference", kv("caseReference", caseReference));

    Case caze =
        caseRepo
            .findByCaseRef(caseReference.toString())
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND,
                        "Could not find case for case reference: " + caseReference));

    log.debug("Found case details by case reference", kv("caseReference", caseReference));
    return caze;
  }
}

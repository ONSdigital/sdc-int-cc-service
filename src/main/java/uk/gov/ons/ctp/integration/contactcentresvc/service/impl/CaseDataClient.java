package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@Slf4j
@Service
public class CaseDataClient {
  private CaseRepository caseRepo;
  private MapperFacade mapper;

  public CaseDataClient(CaseRepository caseRepo, MapperFacade mapper) {
    this.caseRepo = caseRepo;
    this.mapper = mapper;
  }

  public CaseContainerDTO getCaseById(UUID caseId, Boolean listCaseEvents) throws CTPException {
    log.debug("Find case details by ID", kv("caseId", caseId));

    Case caze =
        caseRepo
            .findById(caseId)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND, "Could not find case for ID: " + caseId));

    CaseContainerDTO caseDetails = mapper.map(caze, CaseContainerDTO.class);
    log.debug("Found case details for case ID", kv("caseId", caseId));
    return caseDetails;
  }

  public List<CaseContainerDTO> getCaseByUprn(Long uprn, Boolean listCaseEvents)
      throws CTPException {
    log.debug("Find case details by Uprn", kv("uprn", uprn));

    List<Case> cases = caseRepo.findByAddressUprn(uprn.toString());

    log.debug("Found {} case details by Uprn", cases.size(), kv("uprn", uprn));
    return cases.stream().map(c -> mapper.map(c, CaseContainerDTO.class)).collect(toList());
  }

  public CaseContainerDTO getCaseByCaseRef(Long caseReference, Boolean listCaseEvents)
      throws CTPException {
    log.debug("Find case details by case reference", kv("caseReference", caseReference));

    Case caze =
        caseRepo
            .findByCaseRef(caseReference)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND,
                        "Could not find case for case reference: " + caseReference));

    CaseContainerDTO caseDetails = mapper.map(caze, CaseContainerDTO.class);

    log.debug("Found case details by case reference", kv("caseReference", caseReference));
    return caseDetails;
  }
}

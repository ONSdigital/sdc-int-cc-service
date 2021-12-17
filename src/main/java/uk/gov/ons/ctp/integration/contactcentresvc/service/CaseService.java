package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

/** Service responsible for dealing with Cases */
public interface CaseService {

  CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO) throws CTPException;

  /**
   * Return the latest HH, CE and SPG (but not HI) case at the address given by the Sample key/value
   * pair.
   *
   * @param key Sample attribute to search
   * @param value to search for Cases to return
   * @param requestParamsDTO request details
   * @return List of Cases at address, excluding HI cases
   * @throws CTPException error querying for case
   */
  List<CaseDTO> getCaseBySampleAttribute(
      String key, String value, CaseQueryRequestDTO requestParamsDTO) throws CTPException;

  CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException;

  CaseDTO modifyCase(ModifyCaseRequestDTO modifyRequestDTO) throws CTPException;

  String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException;

  ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException;

  ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;

  ResponseDTO reportRefusal(UUID caseId, @Valid RefusalRequestDTO requestBodyDTO)
      throws CTPException;
}

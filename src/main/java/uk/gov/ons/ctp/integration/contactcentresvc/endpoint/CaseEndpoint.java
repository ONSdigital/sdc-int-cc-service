package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.InteractionService;

/** The REST controller for ContactCentreSvc find cases end points */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint implements CTPEndpoint {
  private CaseService caseService;
  private InteractionService interactionService;

  /**
   * Constructor for ContactCentreDataEndpoint
   *
   * @param caseService is a service layer object that we be doing the processing on behalf of this
   *     endpoint.
   */
  @Autowired
  public CaseEndpoint(final CaseService caseService, final InteractionService interactionService) {
    this.caseService = caseService;
    this.interactionService = interactionService;
  }

  /**
   * the GET end point to get a Case by caseId
   *
   * @param caseId the id of the case
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseById(
      @PathVariable("caseId") final UUID caseId, @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.info(
        "Entering GET getCaseById", kv("pathParam", caseId), kv("requestParams", requestParamsDTO));

    saveCaseInteraction(caseId, CaseInteractionType.MANUAL_CASE_VIEW.name(), null, null);

    CaseDTO result = caseService.getCaseById(caseId, requestParamsDTO);

    return ResponseEntity.ok(result);
  }

  /**
   * the GET end point to get a Case by a Sample attribute
   *
   * @param key the attribute key to search
   * @param value the attribute value to search
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/attribute/{key}/{value}", method = RequestMethod.GET)
  public ResponseEntity<List<CaseDTO>> getCaseByAttribute(
      @PathVariable("key") String key,
      @PathVariable("value") String value,
      @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.info(
        "Entering GET getCaseBySampleAttribute",
        kv("key", key),
        kv("value", value),
        kv("requestParams", requestParamsDTO));

    List<CaseDTO> results = caseService.getCaseBySampleAttribute(key, value, requestParamsDTO);

    //TODO Interactions will possibly be recorded once it's determined how this endpoint will be used

    return ResponseEntity.ok(results);
  }

  /**
   * the GET end point to get a Case by Case Ref
   *
   * @param ref the CaseRef
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/ref/{ref}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseByCaseReference(
      @PathVariable(value = "ref") final long ref, @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.info(
        "Entering GET getCaseByCaseReference",
        kv("pathParam", ref),
        kv("requestParams", requestParamsDTO));

    CaseDTO result = caseService.getCaseByCaseReference(ref, requestParamsDTO);

    saveCaseInteraction(result.getId(), CaseInteractionType.MANUAL_CASE_VIEW.name(), null, null);

    return ResponseEntity.ok(result);
  }

  /**
   * the GET end point to get an EQ Launch URL for a case
   *
   * @param caseId the id of the case
   * @param requestParamsDTO contains request params
   * @return the URL to launch the questionnaire for the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/launch", method = RequestMethod.GET)
  public ResponseEntity<String> getLaunchURLForCaseId(
      @PathVariable(value = "caseId") final UUID caseId, @Valid LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    // INFO because we need to log agent-id
    log.info(
        "Entering GET getLaunchURLForCaseId",
        kv("pathParam", caseId),
        kv("requestParams", requestParamsDTO));

    saveCaseInteraction(caseId, CaseInteractionType.TELEPHONE_CAPTURE_STARTED.name(), null, null);

    String launchURL = caseService.getLaunchURLForCaseId(caseId, requestParamsDTO);

    return ResponseEntity.ok(launchURL);
  }

  /**
   * the POST end point to request a postal fulfilment for a case
   *
   * @param caseId the id of the case
   * @param requestBodyDTO contains request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestByPost(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    log.info(
        "Entering POST fulfilmentRequestByPost",
        kv("pathParam", caseId),
        kv("requestBody", requestBodyDTO));

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId());

    saveCaseInteraction(
        caseId,
        CaseInteractionType.FULFILMENT_REQUESTED.name(),
        CaseSubInteractionType.FULFILMENT_PRINT.name(),
        null);

    ResponseDTO response = caseService.fulfilmentRequestByPost(requestBodyDTO);
    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to request an SMS fulfilment for a case
   *
   * @param caseId the id of the case
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestBySMS(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    log.info(
        "Entering POST fulfilmentRequestBySMS",
        kv("pathParam", caseId),
        kv("requestBody", requestBodyDTO));

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId());

    saveCaseInteraction(
        caseId,
        CaseInteractionType.FULFILMENT_REQUESTED.name(),
        CaseSubInteractionType.FULFILMENT_SMS.name(),
        null);

    ResponseDTO response = caseService.fulfilmentRequestBySMS(requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to report a refusal.
   *
   * @param caseId is the case to log the refusal against.
   * @param requestBodyDTO the request body.
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/refusal", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> reportRefusal(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody RefusalRequestDTO requestBodyDTO)
      throws CTPException {

    log.info(
        "Entering POST reportRefusal", kv("pathParam", caseId), kv("requestBody", requestBodyDTO));

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.warn("reportRefusal caseId in path and body must be identical", kv("caseId", caseId));
      throw new CTPException(
          Fault.BAD_REQUEST, "reportRefusal caseId in path and body must be identical");
    }

    saveCaseInteraction(caseId, CaseInteractionType.REFUSAL_REQUESTED.name(), null, null);

    ResponseDTO response = caseService.reportRefusal(caseId, requestBodyDTO);

    log.debug("Exiting reportRefusal", kv("caseId", caseId));

    return ResponseEntity.ok(response);
  }

  /**
   * The PUT endpoint to modify an existing case.
   *
   * <p>The behaviour is nuanced, since when the CaseType fundamentally changes, then instead of an
   * update (resulting in an ADDRESS_MODIFIED event being sent), a new caseId will be generated and
   * an ADDRESS_TYPE_CHANGED event will be sent instead.
   *
   * @param caseId case ID
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}", method = RequestMethod.PUT)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<CaseDTO> modifyCase(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody ModifyCaseRequestDTO requestBodyDTO)
      throws CTPException {
    log.info("Entering PUT modifyCase", kv("requestBody", requestBodyDTO));

    saveCaseInteraction(caseId, CaseInteractionType.CASE_UPDATE_REQUESTED.name(), null, null);

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId());
    CaseDTO result = caseService.modifyCase(requestBodyDTO);
    return ResponseEntity.ok(result);
  }

  /**
   * the POST end point to log a CaseInteraction
   *
   * @param caseId the id of the case
   * @param requestBodyDTO contains request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/interaction", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> acceptCaseInteraction(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody CaseInteractionDTO requestBodyDTO)
      throws CTPException {

    log.info(
        "Entering POST acceptCaseInteraction",
        kv("pathParam", caseId),
        kv("requestBody", requestBodyDTO));

    // TODO split into separate erros?
    if (!validateInteractionType(requestBodyDTO)) {
      String message = "The Interaction type failed validation";
      log.warn(message, kv("caseId", caseId));
      throw new CTPException(Fault.VALIDATION_FAILED, message);
    }
    ResponseDTO response = interactionService.saveCaseInteraction(caseId, requestBodyDTO);
    return ResponseEntity.ok(response);
  }

  private void validateMatchingCaseId(UUID caseId, UUID dtoCaseId) throws CTPException {
    if (!caseId.equals(dtoCaseId)) {
      String message = "The caseid in the URL does not match the caseid in the request body";
      log.warn(message, kv("caseId", caseId));
      throw new CTPException(Fault.BAD_REQUEST, message);
    }
  }

  private boolean validateInteractionType(CaseInteractionDTO caseInteractionDTO) {
    if (Arrays.stream(CaseInteractionType.values())
        .anyMatch((t) -> t.name().equals(caseInteractionDTO.getType()))) {
      if (CaseInteractionType.valueOf(caseInteractionDTO.getType()).isExplicit()) {
        return caseInteractionDTO.getSubtype() != null
            && CaseInteractionType.valueOf(caseInteractionDTO.getType()).getValidSubInteractions()
                .stream()
                .anyMatch((t) -> t.name().equals(caseInteractionDTO.getSubtype()));
      }
    }
    return false;
  }

  private void saveCaseInteraction(UUID caseId, String type, String subtype, String note) {
    log.info("Saving case interaction", kv("caseId", caseId));
    CaseInteractionDTO dto =
        CaseInteractionDTO.builder().type(type).subtype(subtype).note(note).build();
    interactionService.saveCaseInteraction(caseId, dto);
  }
}

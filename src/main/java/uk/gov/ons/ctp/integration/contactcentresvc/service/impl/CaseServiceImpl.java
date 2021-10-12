package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.v;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.ContactCompact;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.BlacklistedUPRNBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSPostcodesBean;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventTransfer;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpEncrypt;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {
  private static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  @Autowired private AppConfig appConfig;

  @Autowired private CaseDataClient caseDataClient;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired private ProductReference productReference;

  @Autowired private MapperFacade mapper;

  @Autowired private EqLaunchService eqLaunchService;

  @Autowired private EventTransfer eventTransfer;

  @Autowired private CCSPostcodesBean ccsPostcodesBean;

  @Autowired private BlacklistedUPRNBean blacklistedUPRNBean;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  private LuhnCheckDigit luhnChecker = new LuhnCheckDigit();

  public ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    if (log.isDebugEnabled()) {
      log.debug(
          "Now in the fulfilmentRequestByPost method in class CaseServiceImpl.",
          kv("requestBodyDTO", requestBodyDTO));
    }

    verifyFulfilmentCodeNotBlackListed(requestBodyDTO.getFulfilmentCode());

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());

    FulfilmentRequest fulfilmentRequestPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.POST, caseId, contact);

    sendEvent(TopicType.FULFILMENT, fulfilmentRequestPayload, caseId);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.debug(
          "Now returning from the fulfilmentRequestByPost method in class CaseServiceImpl.",
          kv("response", response));
    }

    return response;
  }

  public ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Now in the fulfilmentRequestBySMS method in class CaseServiceImpl.",
          kv("requestBodyDTO", requestBodyDTO));
    }

    verifyFulfilmentCodeNotBlackListed(requestBodyDTO.getFulfilmentCode());

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());

    FulfilmentRequest fulfilmentRequestedPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.SMS, caseId, contact);
    sendEvent(TopicType.FULFILMENT, fulfilmentRequestedPayload, caseId);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.debug(
          "Now returning from the fulfilmentRequestBySMS method in class CaseServiceImpl",
          kv("response", response));
    }

    return response;
  }

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching case details by caseId: {}", caseId);
    }

    CaseDTO caseServiceResponse = mapCaseToDto(getCaseFromDb(caseId));
    caseServiceResponse.setCaseEvents(Collections.emptyList()); // for now there are no case events

    if (log.isDebugEnabled()) {
      log.debug("Returning case details for caseId", kv("caseId", caseId));
    }

    return caseServiceResponse;
  }

  @Override
  public List<CaseDTO> getCaseByUPRN(
      UniquePropertyReferenceNumber uprn, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching latest case details by UPRN", kv("uprn", uprn));
    }

    return callCaseSvcByUPRN(uprn.getValue(), requestParamsDTO.getCaseEvents());
  }

  @Override
  public List<CaseDTO> getCCSCaseByPostcode(String postcode) throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching ccs case details by postcode", kv("postcode", postcode));
    }
    validatePostcode(postcode);

    List<CaseContainerDTO> ccsCaseContainersList = getCcsCasesFromRm(postcode);

    List<CaseDTO> ccsCases = new ArrayList<CaseDTO>();
    for (CaseContainerDTO ccsCaseDetails : ccsCaseContainersList) {
      // Decide if the case type indicates it's worth returning the case
      boolean isSupportedCaseType = false;
      String caseType = ccsCaseDetails.getCaseType();
      if (caseType != null) {
        isSupportedCaseType =
            caseType.equals(CaseType.HH.name())
                || caseType.equals(CaseType.CE.name())
                || caseType.equals(CaseType.SPG.name());
      }

      if (isSupportedCaseType) {
        ccsCases.add(mapper.map(ccsCaseDetails, CaseDTO.class));
      } else {
        log.info(
            "Not returning CCS case with unsupported case type",
            kv("caseId", ccsCaseDetails.getId()),
            kv("caseType", caseType));
      }
    }

    return ccsCases;
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching case details by case reference", kv("caseRef", caseRef));
    }

    validateCaseRef(caseRef);

    Case caseDetails = getCaseFromDb(caseRef);
    CaseDTO caseServiceResponse = mapCaseToDto(caseDetails);
    caseServiceResponse.setCaseEvents(Collections.emptyList()); // no case events for now
    if (log.isDebugEnabled()) {
      log.debug("Returning case details for case reference", kv("caseRef", caseRef));
    }

    // Return a 404 if the UPRN is blacklisted
    UniquePropertyReferenceNumber foundUprn = caseServiceResponse.getUprn();
    if (blacklistedUPRNBean.isUPRNBlacklisted(foundUprn)) {
      log.info(
          "UPRN is blacklisted. Not returning case",
          kv("case", caseServiceResponse.getId()),
          kv("foundUprn", foundUprn));
      throw new CTPException(
          Fault.RESOURCE_NOT_FOUND,
          "Case "
              + caseServiceResponse.getId()
              + " with UPRN "
              + foundUprn.getValue()
              + " is IVR restricted");
    }

    return caseServiceResponse;
  }

  @Override
  public CaseDTO modifyCase(ModifyCaseRequestDTO modifyRequestDTO) throws CTPException {
    validateCompatibleEstabAndCaseType(
        modifyRequestDTO.getCaseType(), modifyRequestDTO.getEstabType());
    UUID originalCaseId = modifyRequestDTO.getCaseId();
    UUID caseId = originalCaseId;

    Case caseDetails = getCaseFromDb(originalCaseId);

    CaseDTO response = mapper.map(caseDetails, CaseDTO.class);
    response.setCaseEvents(Collections.emptyList()); // for now there are no case events.
    String caseRef = caseDetails.getCaseRef();

    // removed most of the code from original Census code since it relies heavily
    // on CaseType/EstabType.

    prepareModificationResponse(response, modifyRequestDTO, caseId, caseRef);
    return response;
  }

  @Override
  public ResponseDTO reportRefusal(UUID caseId, RefusalRequestDTO requestBodyDTO)
      throws CTPException {
    String reportedDateTime = "null";
    if (requestBodyDTO.getDateTime() != null) {
      reportedDateTime = DateTimeUtil.formatDate(requestBodyDTO.getDateTime());
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "Processing refusal for case with reported dateTime",
          kv("caseId", caseId),
          kv("reportedDateTime", reportedDateTime));
    }

    // Create and publish a respondent refusal event
    RefusalDetails refusalPayload = createRespondentRefusalPayload(caseId, requestBodyDTO);

    sendEvent(TopicType.REFUSAL, refusalPayload, caseId);

    // Build response
    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.debug("Returning refusal response for case", kv("caseId", caseId));
    }

    return response;
  }

  @Override
  public String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Processing request to create launch URL",
          kv("caseId", caseId),
          kv("request", requestParamsDTO));
    }

    Case caseDetails = getLaunchCase(caseId);

    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto =
        getNewQidForCase(caseDetails, requestParamsDTO.getIndividual());

    String questionnaireId = newQuestionnaireIdDto.getQuestionnaireId();
    String formType = newQuestionnaireIdDto.getFormType();

    String eqUrl = createLaunchUrl(formType, caseDetails, requestParamsDTO, questionnaireId);
    publishSurveyLaunchedEvent(caseDetails.getId(), questionnaireId, requestParamsDTO.getAgentId());
    return eqUrl;
  }

  @Override
  public UACResponseDTO getUACForCaseId(UUID caseId, UACRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Processing request to get UAC for Case",
          kv("caseId", caseId),
          kv("request", requestParamsDTO));
    }

    Case caseDetails = getLaunchCase(caseId);

    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto =
        getNewQidForCase(caseDetails, requestParamsDTO.getIndividual());

    return UACResponseDTO.builder()
        .id(newQuestionnaireIdDto.getQuestionnaireId())
        .uac(newQuestionnaireIdDto.getUac())
        .dateTime(DateTimeUtil.nowUTC())
        .build();
  }

  private void publishSurveyLaunchedEvent(UUID caseId, String questionnaireId, Integer agentId) {
    log.info(
        "Generating SurveyLaunched event",
        kv("questionnaireId", questionnaireId),
        kv("caseId", caseId),
        kv("agentId", agentId));

    SurveyLaunchResponse response =
        SurveyLaunchResponse.builder()
            .questionnaireId(questionnaireId)
            .caseId(caseId)
            .agentId(Integer.toString(agentId))
            .build();

    sendEvent(TopicType.SURVEY_LAUNCH, response, response.getCaseId());
  }

  private Region convertRegion(Case caze) {
    return Region.valueOf(caze.getAddress().getRegion().name());
  }

  /**
   * create a contact centre fulfilment request event
   *
   * @param fulfilmentCode the code for the product requested
   * @param deliveryChannel how the fulfilment should be delivered
   * @param caseId the id of the household,CE or SPG case the fulfilment is for
   * @return the request event to be delivered to the events exchange
   * @throws CTPException the requested product is invalid for the parameters given
   */
  private FulfilmentRequest createFulfilmentRequestPayload(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel, UUID caseId, Contact contact)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Entering createFulfilmentEvent method in class CaseServiceImpl",
          kv("fulfilmentCode", fulfilmentCode));
    }

    Case caze = getCaseFromDb(caseId);
    Product product = findProduct(fulfilmentCode, deliveryChannel, convertRegion(caze));

    if (deliveryChannel == Product.DeliveryChannel.POST) {
      if (product.getIndividual()) {
        if (StringUtils.isBlank(contact.getForename())
            || StringUtils.isBlank(contact.getSurname())) {

          log.warn(
              "Individual fields are required for the requested fulfilment",
              kv("fulfilmentCode", fulfilmentCode));
          throw new CTPException(
              Fault.BAD_REQUEST,
              "The fulfilment is for an individual so none of the following fields can be empty: "
                  + "'forename' or 'surname'");
        }
      }
    }

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    if (product.getIndividual()) {
      fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
    }

    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);

    return fulfilmentRequest;
  }

  /**
   * find the product using the parameters provided
   *
   * @param fulfilmentCode the code for the product requested
   * @param deliveryChannel how should the fulfilment be delivered
   * @param region identifies the region of the household case the fulfilment is for - used to
   *     confirm the requested products eligibility
   * @return the matching product
   * @throws CTPException the product could not found or is ineligible for the given parameters
   */
  private Product findProduct(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel, Product.Region region)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Passing fulfilmentCode, deliveryChannel, and region, into findProduct method.",
          kv("fulfilmentCode", fulfilmentCode),
          kv("deliveryChannel", deliveryChannel),
          kv("region", region));
    }
    Product searchCriteria =
        Product.builder()
            .fulfilmentCode(fulfilmentCode)
            .requestChannels(Arrays.asList(Product.RequestChannel.CC))
            .deliveryChannel(deliveryChannel)
            .regions(Arrays.asList(region))
            .build();
    List<Product> products = productReference.searchProducts(searchCriteria);
    if (products.size() == 0) {
      log.warn("Compatible product cannot be found", kv("searchCriteria", searchCriteria));
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    return products.get(0);
  }

  private Case getCaseFromDb(UUID caseId) throws CTPException {
    return caseDataClient.getCaseById(caseId);
  }

  private Case getCaseFromDb(long caseRef) throws CTPException {
    return caseDataClient.getCaseByCaseRef(caseRef);
  }

  private List<Case> getCasesFromDb(long uprn) throws CTPException {
    return caseDataClient.getCaseByUprn(uprn);
  }

  private List<CaseContainerDTO> getCcsCasesFromRm(String postcode) {
    List<CaseContainerDTO> caseList = caseServiceClient.getCcsCaseByPostcode(postcode);
    return caseList;
  }

  /**
   * Create a case refusal event.
   *
   * @param caseId is the UUID for the case, or null if the endpoint was invoked with a caseId of
   *     'unknown'.
   * @param refusalRequest holds the details about the refusal.
   * @return the request event to be delivered to the events exchange.
   * @throws CTPException if there is a failure.
   */
  private RefusalDetails createRespondentRefusalPayload(
      UUID caseId, RefusalRequestDTO refusalRequest) throws CTPException {

    // Create message payload
    RefusalDetails refusal = new RefusalDetails();
    refusal.setType(mapToType(refusalRequest.getReason()));
    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);
    refusal.setCollectionCase(collectionCase);
    refusal.setAgentId(Integer.toString(refusalRequest.getAgentId()));
    refusal.setCallId(refusalRequest.getCallId());
    refusal.setHouseholder(refusalRequest.getIsHouseholder());

    // Populate contact
    ContactCompact contact = createRefusalContact(refusalRequest);
    refusal.setContact(contact);

    // Populate address
    AddressCompact address = new AddressCompact();
    address.setAddressLine1(refusalRequest.getAddressLine1());
    address.setAddressLine2(refusalRequest.getAddressLine2());
    address.setAddressLine3(refusalRequest.getAddressLine3());
    address.setTownName(refusalRequest.getTownName());
    address.setPostcode(refusalRequest.getPostcode());
    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region region =
        refusalRequest.getRegion();
    if (region != null) {
      address.setRegion(region.name());
    }
    UniquePropertyReferenceNumber uprn = refusalRequest.getUprn();
    if (uprn != null) {
      address.setUprn(Long.toString(uprn.getValue()));
    }
    refusal.setAddress(address);

    return refusal;
  }

  private ContactCompact createRefusalContact(RefusalRequestDTO refusalRequest) {
    ContactCompact contact = null;

    if (refusalRequest.getReason() == Reason.HARD) {
      contact = new ContactCompact();
      contact.setTitle(encrypt(refusalRequest.getTitle()));
      contact.setForename(encrypt(refusalRequest.getForename()));
      contact.setSurname(encrypt(refusalRequest.getSurname()));
    }
    return contact;
  }

  private String encrypt(String clearValue) {
    if (clearValue == null) {
      return null;
    }
    List<Resource> keys = List.of(appConfig.getPublicPgpKey1(), appConfig.getPublicPgpKey2());
    String encStr = PgpEncrypt.encrypt(clearValue, keys);
    return Base64.getEncoder().encodeToString(encStr.getBytes(StandardCharsets.UTF_8));
  }

  private String mapToType(Reason reason) throws CTPException {
    switch (reason) {
      case HARD:
        return "HARD_REFUSAL";
      case EXTRAORDINARY:
        return "EXTRAORDINARY_REFUSAL";
      default:
        throw new CTPException(Fault.SYSTEM_ERROR, "Unexpected refusal reason: %s", reason);
    }
  }

  /**
   * Make Case Service request to return cases by UPRN
   *
   * @param uprn of requested cases
   * @param listCaseEvents boolean of whether require case events
   * @return List of cases for UPRN
   * @throws CTPException
   */
  private List<CaseDTO> callCaseSvcByUPRN(Long uprn, Boolean listCaseEvents) throws CTPException {

    List<Case> dbCases = new ArrayList<>();
    try {
      dbCases = getCasesFromDb(uprn);
    } catch (CTPException ex) {
      if (ex.getFault() == Fault.RESOURCE_NOT_FOUND) {
        log.info("Case by UPRN Not Found calling Case Service", kv("uprn", uprn));
        return Collections.emptyList();
      } else {
        log.error("Error calling Case Service", kv("uprn", uprn), ex);
        throw ex;
      }
    }
    return mapCaseToDtoList(dbCases);
  }

  private void sendEvent(TopicType topicType, EventPayload payload, Object caseId) {
    UUID transferId = eventTransfer.send(topicType, payload);
    if (log.isDebugEnabled()) {
      log.debug(
          "{} event published",
          v("topicType", topicType),
          kv("caseId", caseId),
          kv("transferId", transferId));
    }
  }

  private void validatePostcode(String postcode) throws CTPException {
    if (!ccsPostcodesBean.isInCCSPostcodes(postcode)) {
      log.info("Check failed for postcode", kv("postcode", postcode));
      throw new CTPException(
          Fault.BAD_REQUEST, "The requested postcode is not within the CCS sample");
    }
  }

  private CaseDTO mapCaseToDto(Case caze) {
    CaseDTO caseServiceResponse = mapper.map(caze, CaseDTO.class);
    caseServiceResponse.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
    return caseServiceResponse;
  }

  private List<CaseDTO> mapCaseToDtoList(List<Case> casesToReturn) {
    List<CaseDTO> dtoList = mapper.mapAsList(casesToReturn, CaseDTO.class);
    for (CaseDTO dto : dtoList) {
      dto.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
      dto.setCaseEvents(Collections.emptyList());
    }
    return dtoList;
  }

  private void validateCaseRef(long caseRef) throws CTPException {
    if (!luhnChecker.isValid(Long.toString(caseRef))) {
      log.info("Luhn check failed for case Reference", kv("caseRef", caseRef));
      throw new CTPException(Fault.BAD_REQUEST, "Invalid Case Reference");
    }
  }

  private void validateCompatibleEstabAndCaseType(CaseType caseType, EstabType estabType)
      throws CTPException {
    Optional<AddressType> addrType = estabType.getAddressType();
    if (addrType.isPresent() && (caseType != CaseType.valueOf(addrType.get().name()))) {
      log.info(
          "Mismatching caseType and estabType",
          kv("caseType", caseType),
          kv("estabType", estabType));
      String msg =
          "Derived address type of '"
              + addrType.get()
              + "', from establishment type '"
              + estabType
              + "', "
              + "is not compatible with caseType of '"
              + caseType
              + "'";
      throw new CTPException(Fault.BAD_REQUEST, msg);
    }
  }

  private void prepareModificationResponse(
      CaseDTO response, ModifyCaseRequestDTO modifyRequestDTO, UUID caseId, String caseRef) {
    response.setId(caseId);
    response.setCaseRef(caseRef);
    response.setAddressLine1(modifyRequestDTO.getAddressLine1());
    response.setAddressLine2(modifyRequestDTO.getAddressLine2());
    response.setAddressLine3(modifyRequestDTO.getAddressLine3());
    response.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
    response.setCaseEvents(Collections.emptyList());
  }

  /**
   * Request a new questionnaire Id for a Case
   *
   * @param caseDetails of case for which to get questionnaire Id
   * @param individual whether request for individual questionnaire
   * @return
   */
  private SingleUseQuestionnaireIdDTO getNewQidForCase(Case caseDetails, boolean individual)
      throws CTPException {

    UUID parentCaseId = caseDetails.getId();
    UUID individualCaseId = null;
    if (individual) {
      caseDetails = createIndividualCase(caseDetails);
    }

    // Get RM to allocate a new questionnaire ID
    log.info("Before new QID");
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto;
    try {
      newQuestionnaireIdDto =
          caseServiceClient.getSingleUseQuestionnaireId(parentCaseId, individual, individualCaseId);
    } catch (ResponseStatusException ex) {
      if (ex.getCause() != null) {
        HttpStatusCodeException cause = (HttpStatusCodeException) ex.getCause();
        log.warn(
            "New QID, UAC Case service request failure.",
            kv("caseid", parentCaseId),
            kv("status", cause.getStatusCode()),
            kv("message", cause.getMessage()));
        if (cause.getStatusCode() == HttpStatus.BAD_REQUEST) {
          throw new CTPException(
              Fault.BAD_REQUEST, "Invalid request for case %s", parentCaseId.toString());
        }
      }
      throw ex;
    }

    String questionnaireId = newQuestionnaireIdDto.getQuestionnaireId();
    String formType = newQuestionnaireIdDto.getFormType();
    log.info(
        "Have generated new questionnaireId",
        kv("newQuestionnaireID", questionnaireId),
        kv("formType", formType));

    return newQuestionnaireIdDto;
  }

  // Create a new case for a HH individual
  private Case createIndividualCase(Case caseDetails) {
    UUID individualCaseId = UUID.randomUUID();
    caseDetails.setId(individualCaseId);
    log.info("Creating new individual case", kv("individualCaseId", individualCaseId));
    return caseDetails;
  }

  /**
   * Get the Case for which the client has requested a launch URL/UAC
   *
   * @param caseId of case to get
   * @return Case for case requested
   * @throws CTPException if case not available to call (not in case service)
   */
  private Case getLaunchCase(UUID caseId) throws CTPException {
    try {
      return getCaseFromDb(caseId);
    } catch (CTPException ex) {
      log.error(
          "Unable to provide launch URL/UAC, failed to find case in DB", kv("caseId", caseId), ex);
      throw ex;
    }
  }

  private String createLaunchUrl(
      String formType, Case caze, LaunchRequestDTO requestParamsDTO, String questionnaireId)
      throws CTPException {
    String encryptedPayload = "";
    CaseContainerDTO caseDetails = mapper.map(caze, CaseContainerDTO.class);
    try {
      EqLaunchData eqLuanchCoreDate =
          EqLaunchData.builder()
              .language(Language.ENGLISH)
              .source(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API)
              .channel(uk.gov.ons.ctp.common.domain.Channel.CC)
              .questionnaireId(questionnaireId)
              .formType(formType)
              .salt(appConfig.getEq().getResponseIdSalt())
              .caseContainer(caseDetails)
              .userId(Integer.toString(requestParamsDTO.getAgentId()))
              .accountServiceUrl(null)
              .accountServiceLogoutUrl(null)
              .build();
      encryptedPayload = eqLaunchService.getEqLaunchJwe(eqLuanchCoreDate);

    } catch (CTPException e) {
      log.error(
          "Failed to create JWE payload for eq launch",
          kv("caseId", caze.getId()),
          kv("questionnaireId", questionnaireId),
          e);
      throw e;
    }
    String eqUrl =
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + encryptedPayload;
    if (log.isDebugEnabled()) {
      log.debug("Have created launch URL", kv("launchURL", eqUrl));
    }
    return eqUrl;
  }

  private void verifyFulfilmentCodeNotBlackListed(String fulfilmentCode) throws CTPException {
    Set<String> blacklistedProducts = appConfig.getFulfilments().getBlacklistedCodes();

    if (blacklistedProducts.contains(fulfilmentCode)) {
      log.info("Fulfilment code is no longer available", kv("fulfilmentCode", fulfilmentCode));
      throw new CTPException(
          Fault.BAD_REQUEST, "Requested fulfilment code is no longer available: " + fulfilmentCode);
    }
  }
}

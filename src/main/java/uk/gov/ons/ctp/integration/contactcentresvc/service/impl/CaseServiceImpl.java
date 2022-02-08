package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.v;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientService;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.TelephoneCaptureDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.BlacklistedUPRNBean;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventTransfer;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseRepositoryClient;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UacRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseSummaryDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpEncrypt;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {

  @Autowired private AppConfig appConfig;

  @Autowired private CaseRepositoryClient caseRepoClient;

  @Autowired private CaseInteractionRepository caseInteractionRepository;

  @Autowired private UacRepository uacRepo;

  @Autowired private CaseServiceClientService caseServiceClient;

  @Autowired private ProductReference productReference;

  @Autowired private MapperFacade mapper;

  @Autowired private EqLaunchService eqLaunchService;

  @Autowired private EventTransfer eventTransfer;

  @Autowired private BlacklistedUPRNBean blacklistedUPRNBean;


  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  private LuhnCheckDigit luhnChecker = new LuhnCheckDigit();

  public Survey getSurveyForCase(UUID caseId) throws CTPException {
    Case caze = caseRepoClient.getCaseById(caseId);
    return caze.getCollectionExercise().getSurvey();
  }

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

    CaseDTO caseServiceResponse = mapCaseToDto(caseRepoClient.getCaseById(caseId));

    List<CaseInteractionDTO> interactions =
        buildInteractionHistory(caseServiceResponse.getId(), requestParamsDTO.getCaseEvents());
    caseServiceResponse.setInteractions(interactions);

    if (log.isDebugEnabled()) {
      log.debug("Returning case details for caseId", kv("caseId", caseId));
    }

    return caseServiceResponse;
  }

  @Override
  public List<CaseDTO> getCaseBySampleAttribute(
      String key, String value, CaseQueryRequestDTO requestParamsDTO) throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching latest case details by {}", key, kv("key", key), kv("value", value));
    }

    List<Case> dbCases = caseRepoClient.getCaseBySampleAttribute(key, value);

    List<CaseDTO> cases = mapper.mapAsList(dbCases, CaseDTO.class);

    // Set interaction history for all cases
    for (CaseDTO caseDTO : cases) {
      List<CaseInteractionDTO> interactions =
          buildInteractionHistory(caseDTO.getId(), requestParamsDTO.getCaseEvents());
      caseDTO.setInteractions(interactions);
    }

    return cases;
  }

  @Override
  public List<CaseSummaryDTO> getCaseSummaryBySampleAttribute(String key, String value)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug(
          "Fetching latest case summary details by {}", key, kv("key", key), kv("value", value));
    }

    // Find matching cases
    List<Case> dbCases;
    dbCases = caseRepoClient.getCaseBySampleAttribute(key, value);

    // Summarise all found cases
    List<CaseSummaryDTO> caseSummaries = new ArrayList<>();
    for (Case dbCase : dbCases) {
      CaseSummaryDTO caseSummary = new CaseSummaryDTO();
      caseSummary.setId(dbCase.getId());
      caseSummary.setCaseRef(dbCase.getCaseRef());

      Survey survey = dbCase.getCollectionExercise().getSurvey();
      caseSummary.setSurveyName(survey.getName());

      SurveyType surveyType = SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl());
      caseSummary.setSurveyType(surveyType.name());

      caseSummaries.add(caseSummary);
    }

    return caseSummaries;
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching case details by case reference", kv("caseRef", caseRef));
    }

    validateCaseRef(caseRef);

    Case caseDetails = caseRepoClient.getCaseByCaseRef(caseRef);
    CaseDTO caseServiceResponse = mapCaseToDto(caseDetails);

    List<CaseInteractionDTO> interactions =
        buildInteractionHistory(caseServiceResponse.getId(), requestParamsDTO.getCaseEvents());
    caseServiceResponse.setInteractions(interactions);

    if (log.isDebugEnabled()) {
      log.debug("Returning case details for case reference", kv("caseRef", caseRef));
    }

    // Return a 404 if the UPRN is blacklisted
    UniquePropertyReferenceNumber foundUprn =
        UniquePropertyReferenceNumber.create(
            caseServiceResponse.getSample().get(CaseUpdate.ATTRIBUTE_UPRN).toString());
    // TODO : FLEXIBLE CASE - should the blacklisting be survey specific
    // TODO : FLEXIBLE CASE - should we blacklist something other than UPRN for a
    // non address survey
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

    Case caseDetails = caseRepoClient.getCaseById(originalCaseId);

    CaseDTO response = mapper.map(caseDetails, CaseDTO.class);

    // removed most of the code from original Census code since it relies heavily
    // on CaseType/EstabType.

    String caseRef = caseDetails.getCaseRef();

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
    log.debug(
        "Processing refusal for case with reported dateTime",
        kv("caseId", caseId),
        kv("reportedDateTime", reportedDateTime));

    // Create and publish a respondent refusal event
    RefusalDetails refusalPayload = createRespondentRefusalPayload(caseId, requestBodyDTO);

    sendEvent(TopicType.REFUSAL, refusalPayload, caseId);

    // Build response
    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.debug("Returning refusal response for case", kv("caseId", caseId));

    return response;
  }

  /**
   * We may want to consider how the calling code (CaseEndpoint) handles the BAD_REQUESTs that come
   * from this method. Throwing exceptions in these scenarios obliges the caller (and perhaps the
   * UI) to first consider the validity of its request, which may be onerous. Perhaps we consider
   * providing the UI with an endpoint that pre validates the ability to launch which would enable
   * it to disable the launch button in the first place.
   */
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

    // we call RM to get a new single use qid for the case
    TelephoneCaptureDTO newQuestionnaireIdDto = getNewQidForCase(caseDetails);
    String questionnaireId = newQuestionnaireIdDto.getQId();

    // But RM do not provide a collectionInstrumentUrl so we will use the collex
    // and calc the current wave
    CollectionExercise collex = caseDetails.getCollectionExercise();
    // the assumption is that the calc should be strict and not allow launch if a wave is no longer
    // operational
    Optional<Integer> waveNumOpt = collex.calcWaveForDate(LocalDateTime.now(), true);
    if (waveNumOpt.isEmpty()) {
      throw new CTPException(
          Fault.BAD_REQUEST, "The CollectionExercise for this Case has no current wave");
    }
    int waveNum = waveNumOpt.get();

    // then we will find the UAC that should have been sent to the respondent and use its
    // instrumentUrl along with the fresh qid obtained from RM.
    List<Uac> uacList = uacRepo.findByCaseId(caseId);
    Optional<Uac> uacOpt =
        uacList.stream().filter(uac -> (uac.getWaveNum() == waveNum)).findFirst();
    if (uacOpt.isEmpty()) {
      throw new CTPException(
          Fault.BAD_REQUEST, "Failed to find pre-existing UAC for the current wave num");
    }
    Uac uac = uacOpt.get();
    uac.setQuestionnaire(questionnaireId);

    String eqUrl = createLaunchUrl(caseDetails, requestParamsDTO, uac);
    publishEqLaunchedEvent(caseDetails.getId(), questionnaireId);
    return eqUrl;
  }

  /*
   * Build history for a case.
   *
   * If caller wants case events then the case history is constructed from the RM case
   * event history, which is amalgamated with the interactions recorded by CC itself.
   *
   * @param caseDTO the case for which we want the history.
   * @param getCaseEvents boolean to indicate if the caller wants the case history.
   * @return a List of case interactions, or an empty list if the caller doesn't want history.
   */
  private List<CaseInteractionDTO> buildInteractionHistory(UUID caseId, Boolean getCaseEvents)
      throws CTPException {
    List<CaseInteractionDTO> interactions = new ArrayList<>();

    if (getCaseEvents) {
      // Get case and event history from RM
      RmCaseDTO rmCase;
      try {
        rmCase = caseServiceClient.getCaseById(caseId, true);
      } catch (ResponseStatusException ex) {
        if (ex.getCause() != null) {
          HttpStatusCodeException cause = (HttpStatusCodeException) ex.getCause();
          log.warn(
              "Failed to get case from RM.",
              kv("caseid", caseId),
              kv("status", cause.getStatusCode()),
              kv("message", cause.getMessage()));
          throw new CTPException(
              Fault.SYSTEM_ERROR, "Error detected when calling RM for case %s", caseId.toString());
        }
        throw ex;
      }

      // Create history from RM events
      for (EventDTO rmCaseEvent : rmCase.getCaseEvents()) {
        interactions.add(createRmInteraction(rmCaseEvent));
      }

      // Add in interactions recorded by CC
      List<CaseInteraction> ccInteractions = caseInteractionRepository.findByCaseId(caseId);
      for (CaseInteraction ccInteraction : ccInteractions) {
        interactions.add(createCcInteraction(ccInteraction));
      }

      // Remove interactions that are not worth reporting
      Set<String> whitelistedEventNames =
          appConfig.getCaseServiceSettings().getWhitelistedEventCategories();
      interactions =
          interactions.stream()
              .filter(i -> whitelistedEventNames.contains(i.getInteraction()))
              .collect(Collectors.toList());

      // Sort, so that newest interactions appear first
      interactions.sort(Comparator.comparing(CaseInteractionDTO::getCreatedDateTime).reversed());
    }

    return interactions;
  }

  private CaseInteractionDTO createRmInteraction(EventDTO rmCaseEvent) {
    return CaseInteractionDTO.builder()
        .interactionSource("RM")
        .interaction(rmCaseEvent.getEventType())
        .subInteraction("")
        .note(rmCaseEvent.getDescription())
        .createdDateTime(rmCaseEvent.getCreatedDateTime())
        .userName("")
        .build();
  }

  private CaseInteractionDTO createCcInteraction(CaseInteraction ccInteraction) {
    return CaseInteractionDTO.builder()
        .interactionSource("CC")
        .interaction(ccInteraction.getType().name())
        .subInteraction(ccInteraction.getSubtype() != null ? ccInteraction.getSubtype().name() : "")
        .note(ccInteraction.getNote() != null ? ccInteraction.getNote() : "")
        .createdDateTime(ccInteraction.getCreatedDateTime())
        .userName(ccInteraction.getCcuser().getName())
        .build();
  }

  private void publishEqLaunchedEvent(UUID caseId, String questionnaireId) {
    log.info(
        "Generating EqLaunched event",
        kv("questionnaireId", questionnaireId),
        kv("caseId", caseId));

    EqLaunch eqLaunch = EqLaunch.builder().qid(questionnaireId).build();

    sendEvent(TopicType.EQ_LAUNCH, eqLaunch, caseId);
  }

  // TODO : FLEXIBLE CASE - what if the survey is not address based?
  private Product.Region convertRegion(Case caze) {
    return Product.Region.valueOf(caze.getSample().get(CaseUpdate.ATTRIBUTE_REGION));
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

    Case caze = caseRepoClient.getCaseById(caseId);
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

    RefusalDetails refusal = new RefusalDetails();
    refusal.setCaseId(caseId);
    refusal.setType(refusalRequest.getReason().name());

    // This code is intentionally commented out. Reinstate to active encryption in
    // outgoing events
    // refusal.setName(encrypt("Jimmy McTavish"));

    // The following code exists to prevent complaints about unused code. It's never
    // called.
    // This can be deleted once a final decision is reached about ccsvc encryption
    if (System.currentTimeMillis() == 1) {
      encrypt("never-executed");
    }

    return refusal;
  }

  private String encrypt(String clearValue) {
    if (clearValue == null) {
      return null;
    }
    List<Resource> keys = List.of(appConfig.getPublicPgpKey1(), appConfig.getPublicPgpKey2());
    String encStr = PgpEncrypt.encrypt(clearValue, keys);
    return Base64.getEncoder().encodeToString(encStr.getBytes(StandardCharsets.UTF_8));
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

  private CaseDTO mapCaseToDto(Case caze) {
    CaseDTO caseServiceResponse = mapper.map(caze, CaseDTO.class);
    return caseServiceResponse;
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

  // TODO : FLEXIBLE CASE - this assumes that modifications are address related
  private void prepareModificationResponse(
      CaseDTO response, ModifyCaseRequestDTO modifyRequestDTO, UUID caseId, String caseRef) {
    response.setId(caseId);
    response.setCaseRef(caseRef);
    response
        .getSample()
        .put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1, modifyRequestDTO.getAddressLine1());
    response
        .getSample()
        .put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_2, modifyRequestDTO.getAddressLine2());
    response
        .getSample()
        .put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_3, modifyRequestDTO.getAddressLine3());
    response.setInteractions(Collections.emptyList());
  }

  /**
   * Request a new questionnaire Id for a Case
   *
   * @param caseDetails of case for which to get questionnaire Id
   * @param individual whether request for individual questionnaire
   * @return
   */
  private TelephoneCaptureDTO getNewQidForCase(Case caseDetails) throws CTPException {

    UUID caseId = caseDetails.getId();

    // Get RM to allocate a new questionnaire ID
    log.info("Before new QID");
    TelephoneCaptureDTO newQuestionnaireIdDto;
    try {
      newQuestionnaireIdDto = caseServiceClient.getSingleUseQuestionnaireId(caseId);
    } catch (ResponseStatusException ex) {
      if (ex.getCause() != null) {
        HttpStatusCodeException cause = (HttpStatusCodeException) ex.getCause();
        log.warn(
            "New QID, UAC Case service request failure.",
            kv("caseid", caseId),
            kv("status", cause.getStatusCode()),
            kv("message", cause.getMessage()));
        if (cause.getStatusCode() == HttpStatus.BAD_REQUEST) {
          throw new CTPException(
              Fault.BAD_REQUEST, "Invalid request for case %s", caseId.toString());
        }
      }
      throw ex;
    }

    String questionnaireId = newQuestionnaireIdDto.getQId();
    log.info("Have generated new questionnaireId", kv("newQuestionnaireID", questionnaireId));

    return newQuestionnaireIdDto;
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
      return caseRepoClient.getCaseById(caseId);
    } catch (CTPException ex) {
      log.error(
          "Unable to provide launch URL/UAC, failed to find case in DB", kv("caseId", caseId), ex);
      throw ex;
    }
  }

  private String createLaunchUrl(Case caze, LaunchRequestDTO requestParamsDTO, Uac uac)
      throws CTPException {

    String encryptedPayload = "";
    CollectionExercise collex = caze.getCollectionExercise();
    Survey survey = collex.getSurvey();

    CaseUpdate caseUpdate = mapper.map(caze, CaseUpdate.class);
    UacUpdate uacUpdate = mapper.map(uac, UacUpdate.class);

    CollectionExerciseUpdate collexUpdate =
        mapper.map(caze.getCollectionExercise(), CollectionExerciseUpdate.class);

    try {
      EqLaunchData eqLaunchData =
          EqLaunchData.builder()
              .language(Language.ENGLISH)
              .source(Source.CONTACT_CENTRE_API)
              .channel(Channel.CC)
              .salt(appConfig.getEq().getResponseIdSalt())
              .surveyType(SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl()))
              .collectionExerciseUpdate(collexUpdate)
              .uacUpdate(uacUpdate)
              .caseUpdate(caseUpdate)
              .userId(Integer.toString(requestParamsDTO.getAgentId()))
              .accountServiceUrl(null)
              .accountServiceLogoutUrl(null)
              .build();
      encryptedPayload = eqLaunchService.getEqLaunchJwe(eqLaunchData);

    } catch (CTPException e) {
      log.error(
          "Failed to create JWE payload for eq launch",
          kv("caseId", caze.getId()),
          kv("questionnaireId", uac.getQuestionnaire()),
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

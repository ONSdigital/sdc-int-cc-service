package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientService;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.contactcentresvc.BlacklistedUPRNBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventTransfer;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseRepositoryClient;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UacRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseSummaryDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public abstract class CaseServiceImplTestBase {
  @Spy AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientService caseServiceClient;

  @Mock CaseRepositoryClient caseDataClient;

  @Mock UacRepository uacRepo;

  @Mock CaseInteractionRepository caseInteractionRepo;

  @Mock EqLaunchService eqLaunchService;

  @Mock EventTransfer eventTransfer;

  @Mock RBACService rbacService;

  @Spy MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Mock BlacklistedUPRNBean blacklistedUPRNBean;

  static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  @InjectMocks CaseService target = new CaseService();

  void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis >= minAllowed, actualInMillis + " not after " + minAllowed);
    assertTrue(actualInMillis <= maxAllowed, actualInMillis + " not before " + maxAllowed);
  }

  long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    return dateParser.parse(datetime).getTime();
  }

  <T extends EventPayload> T verifyEventSent(TopicType expectedEventType, Class<T> payloadClazz) {
    ArgumentCaptor<T> payloadCaptor = ArgumentCaptor.forClass(payloadClazz);
    verify(eventTransfer).send(eq(expectedEventType), payloadCaptor.capture());

    return payloadCaptor.getValue();
  }

  void verifyEventNotSent() {
    verify(eventTransfer, never()).send(any(), any());
  }

  void verifyCase(CaseDTO results, CaseDTO expectedCaseResult) throws Exception {
    assertEquals(expectedCaseResult.getId(), results.getId());
    assertEquals(expectedCaseResult.getCollectionExerciseId(), results.getCollectionExerciseId());
    assertEquals(expectedCaseResult.getSurveyId(), results.getSurveyId());
    assertEquals(expectedCaseResult.getSurveyType(), results.getSurveyType());
    assertEquals(expectedCaseResult.getCaseRef(), results.getCaseRef());
    assertEquals(expectedCaseResult.getSample(), results.getSample());
    assertEquals(expectedCaseResult.getSampleSensitive(), results.getSampleSensitive());
    assertEquals(expectedCaseResult.isCanLaunch(), results.isCanLaunch());

    verifyEventNotSent();
  }

  void verifyInteractions(
      List<CaseInteractionDTO> expectedInteractions, List<CaseInteractionDTO> actualInteractions) {

    assertEquals(expectedInteractions.size(), actualInteractions.size());

    for (int i = 0; i < expectedInteractions.size(); i++) {
      CaseInteractionDTO expected = expectedInteractions.get(0);
      CaseInteractionDTO actual = actualInteractions.get(0);

      assertEquals(expected, actual, "Checking index: " + i);
    }
  }

  CaseDTO createExpectedCaseDTO(Case caseFromDb) {

    Survey survey = caseFromDb.getCollectionExercise().getSurvey();

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromDb.getId())
            .collectionExerciseId(caseFromDb.getCollectionExercise().getId())
            .surveyId(survey.getId())
            .surveyType(SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl()))
            .caseRef(caseFromDb.getCaseRef())
            .sample(new HashMap<>(caseFromDb.getSample()))
            .sampleSensitive(new HashMap<>(caseFromDb.getSampleSensitive()))
            .interactions(Collections.emptyList())
            .canLaunch(true)
            .build();
    return expectedCaseResult;
  }

  CaseSummaryDTO createExpectedCaseSummaryDTO(Case caseFromDb) {

    CaseSummaryDTO expectedCaseResult =
        CaseSummaryDTO.builder()
            .id(caseFromDb.getId())
            .caseRef(caseFromDb.getCaseRef())
            .surveyName(caseFromDb.getCollectionExercise().getSurvey().getName())
            .surveyType("SOCIAL")
            .build();
    return expectedCaseResult;
  }

  UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  void mockGetUacsForCase() throws Exception {
    List<Uac> uacList = FixtureHelper.loadPackageFixtures(Uac[].class);
    when(uacRepo.findByCaseId(any())).thenReturn(uacList);
  }

  Case mockGetCaseById(String region) throws Exception {
    Case caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    caze.getSample().put("region", region);
    CollectionExercise collex = caze.getCollectionExercise();
    collex.setStartDate(LocalDateTime.now());
    collex.setEndDate(LocalDateTime.now().plusDays(60));
    collex.setWaveLength(10);
    collex.setNumberOfWaves(3);
    mockGetCaseById(CASE_ID_0, caze);
    return caze;
  }

  Case mockGetCaseByIdInFutureCollection(String region) throws Exception {
    Case caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    caze.getSample().put("region", region);
    CollectionExercise collex = caze.getCollectionExercise();
    collex.setStartDate(LocalDateTime.now().plusDays(10));
    collex.setEndDate(LocalDateTime.now().plusDays(60));
    collex.setWaveLength(10);
    collex.setNumberOfWaves(3);
    mockGetCaseById(CASE_ID_0, caze);
    return caze;
  }

  Case mockGetCaseByIdAtEndOfCollection(String region) throws Exception {
    Case caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    caze.getSample().put("region", region);
    CollectionExercise collex = caze.getCollectionExercise();
    // collex started some time ago
    collex.setStartDate(LocalDateTime.now().minusDays(60));
    collex.setEndDate(LocalDateTime.now().plusDays(1));
    collex.setWaveLength(10);
    // and the num of waves will put today in a wave for which we have no UAC
    collex.setNumberOfWaves(20);
    mockGetCaseById(CASE_ID_0, caze);
    return caze;
  }

  void mockGetCaseById(UUID id, Case result) throws Exception {
    when(caseDataClient.getCaseById(eq(id))).thenReturn(result);
  }

  void mockGetCaseById(UUID id, Exception ex) throws Exception {
    doThrow(ex).when(caseDataClient).getCaseById(eq(id));
  }

  void mockRmGetCaseDTO(UUID id) {
    RmCaseDTO rmCaseDTO = FixtureHelper.loadPackageFixtures(RmCaseDTO[].class).get(0);

    lenient().when(caseServiceClient.getCaseById(id, true)).thenReturn(rmCaseDTO);
  }

  void mockRmGetCaseDTOFailure(UUID id, HttpStatus httpStatus) {
    Exception causeRmFailure = new HttpServerErrorException(httpStatus, "It's gone wrong");
    Exception rmException =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown case", causeRmFailure);

    when(caseServiceClient.getCaseById(id, true)).thenThrow(rmException);
  }

  void mockCaseInteractionRepoFindByCaseId(UUID id) {
    List<CaseInteraction> caseInteractions =
        FixtureHelper.loadPackageFixtures(CaseInteraction[].class);

    lenient().when(caseInteractionRepo.findByCaze_Id(id)).thenReturn(caseInteractions);
  }

  void mockCaseEventWhiteList() {
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet =
        Set.of(
            "NEW_CASE",
            "PRINT_FULFILMENT",
            "SMS_FULFILMENT",
            "MANUAL_CASE_VIEW",
            "OUTBOUND_MANUAL_CALL");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    lenient().when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }
}

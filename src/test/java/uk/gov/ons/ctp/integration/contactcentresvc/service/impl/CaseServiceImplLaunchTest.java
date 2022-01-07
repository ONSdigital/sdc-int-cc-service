package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.TelephoneCaptureDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.EqConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;

/**
 * Unit Test {@link CaseService#getLaunchURLForCaseId(UUID, LaunchRequestDTO)
 * getLaunchURLForCaseId}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplLaunchTest extends CaseServiceImplTestBase {

  @Captor private ArgumentCaptor<EqLaunchData> eqLaunchDataCaptor;
  @Mock private KeyStore keyStoreEncryption;

  @BeforeEach
  public void setup() {
    EqConfig eqConfig = new EqConfig();
    eqConfig.setProtocol("https");
    eqConfig.setHost("localhost");
    eqConfig.setPath("/en/start/launch-eq/?token=");
    eqConfig.setResponseIdSalt("CENSUS");
    appConfig.setEq(eqConfig);

    Mockito.lenient().when(appConfig.getEq()).thenReturn(eqConfig);
  }

  @Test
  public void testLaunchCaseWithNoUACForWave() throws Exception {
    // setup mocking
    mockGetCaseByIdAtEndOfCollection(A_REGION.name());
    mockGetUacsForCase();

    // Fake RM response for creating questionnaire ID
    TelephoneCaptureDTO newQuestionnaireIdDto = new TelephoneCaptureDTO();
    newQuestionnaireIdDto.setQId(A_QUESTIONNAIRE_ID);
    Mockito.lenient()
        .when(caseServiceClient.getSingleUseQuestionnaireId(eq(CASE_ID_0)))
        .thenReturn(newQuestionnaireIdDto);

    // Mock out building of launch payload
    Mockito.lenient()
        .when(eqLaunchService.getEqLaunchJwe(any(EqLaunchData.class)))
        .thenReturn("simulated-encrypted-payload");

    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);

    assertThrows(
        CTPException.class, () -> target.getLaunchURLForCaseId(CASE_ID_0, launchRequestDTO));
  }

  @Test
  public void testLaunchCaseBeforeCollexStarts() throws Exception {
    // setup mocking
    mockGetCaseByIdInFutureCollection(A_REGION.name());

    // Fake RM response for creating questionnaire ID
    TelephoneCaptureDTO newQuestionnaireIdDto = new TelephoneCaptureDTO();
    newQuestionnaireIdDto.setQId(A_QUESTIONNAIRE_ID);
    Mockito.lenient()
        .when(caseServiceClient.getSingleUseQuestionnaireId(eq(CASE_ID_0)))
        .thenReturn(newQuestionnaireIdDto);

    // Mock out building of launch payload
    Mockito.lenient()
        .when(eqLaunchService.getEqLaunchJwe(any(EqLaunchData.class)))
        .thenReturn("simulated-encrypted-payload");

    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);

    assertThrows(
        CTPException.class, () -> target.getLaunchURLForCaseId(CASE_ID_0, launchRequestDTO));
  }

  @Test
  public void testLaunchCase() throws Exception {
    // setup mocking
    mockGetCaseById(A_REGION.name());
    mockGetUacsForCase();

    // Fake RM response for creating questionnaire ID
    TelephoneCaptureDTO newQuestionnaireIdDto = new TelephoneCaptureDTO();
    newQuestionnaireIdDto.setQId(A_QUESTIONNAIRE_ID);
    Mockito.lenient()
        .when(caseServiceClient.getSingleUseQuestionnaireId(eq(CASE_ID_0)))
        .thenReturn(newQuestionnaireIdDto);

    // Mock out building of launch payload
    Mockito.lenient()
        .when(eqLaunchService.getEqLaunchJwe(any(EqLaunchData.class)))
        .thenReturn("simulated-encrypted-payload");

    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);

    // Invoke method under test, and check returned url
    String launchUrl = target.getLaunchURLForCaseId(CASE_ID_0, launchRequestDTO);
    assertEquals(
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + "simulated-encrypted-payload",
        launchUrl);

    verifyEqLaunchJwe(A_QUESTIONNAIRE_ID);
    verifyEqLaunchedEventPublished(CASE_ID_0, A_QUESTIONNAIRE_ID);
  }

  @Test
  public void testLaunch_caseServiceNotFoundException() throws Exception {
    mockGetCaseById(CASE_ID_0, new ResponseStatusException(HttpStatus.NOT_FOUND));
    assertThrows(
        ResponseStatusException.class,
        () -> target.getLaunchURLForCaseId(CASE_ID_0, new LaunchRequestDTO()));
  }

  @Test
  public void testLaunch_caseServiceResponseStatusException() throws Exception {
    mockGetCaseById(CASE_ID_0, new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT));
    assertThrows(
        ResponseStatusException.class,
        () -> target.getLaunchURLForCaseId(CASE_ID_0, new LaunchRequestDTO()));
  }

  private void verifyEqLaunchJwe(String questionnaireId) throws Exception {
    Mockito.verify(eqLaunchService).getEqLaunchJwe(eqLaunchDataCaptor.capture());
    EqLaunchData eqLaunchData = eqLaunchDataCaptor.getValue();

    assertEquals(Language.ENGLISH, eqLaunchData.getLanguage());
    assertEquals(Source.CONTACT_CENTRE_API, eqLaunchData.getSource());
    assertEquals(Channel.CC, eqLaunchData.getChannel());
    assertEquals(AN_AGENT_ID, eqLaunchData.getUserId());
    assertEquals(questionnaireId, eqLaunchData.getUacUpdate().getQid());
    assertNull(eqLaunchData.getAccountServiceLogoutUrl());
    assertNull(eqLaunchData.getAccountServiceUrl());
    assertEquals(CASE_ID_0, UUID.fromString(eqLaunchData.getCaseUpdate().getCaseId()));
  }

  private void verifyEqLaunchedEventPublished(UUID caseId, String questionnaireId) {
    EqLaunch payloadSent = verifyEventSent(TopicType.EQ_LAUNCH, EqLaunch.class);
    assertEquals(questionnaireId, payloadSent.getQid());
  }
}

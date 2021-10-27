package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
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
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
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

  @Captor private ArgumentCaptor<UUID> individualCaseIdCaptor;
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
  public void testLaunchHHCase() throws Exception {
    doLaunchTest(false);
  }

  @Test
  public void testLaunchHHCaseForIndividual() throws Exception {
    doLaunchTest(true);
  }

  @Test
  public void testLaunch_caseServiceNotFoundException() throws Exception {
    mockGetCaseById(UUID_0, new ResponseStatusException(HttpStatus.NOT_FOUND));
    assertThrows(
        ResponseStatusException.class,
        () -> target.getLaunchURLForCaseId(UUID_0, new LaunchRequestDTO()));
  }

  @Test
  public void testLaunch_caseServiceResponseStatusException() throws Exception {
    mockGetCaseById(UUID_0, new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT));
    assertThrows(
        ResponseStatusException.class,
        () -> target.getLaunchURLForCaseId(UUID_0, new LaunchRequestDTO()));
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(
      CaseContainerDTO dto, String expectedMsg, Fault expectedFault) {
    CTPException e = assertThrows(CTPException.class, () -> doLaunchTest(false, FormType.C));
    assertEquals(expectedFault, e.getFault());
    assertTrue(e.getMessage().contains(expectedMsg), e.getMessage());
  }

  @SneakyThrows
  private void assertThatCeManagerFormFromUnitRegionIsRejected(CaseContainerDTO dto) {
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "A CE Manager form can only be launched against an establishment address not a UNIT.",
        Fault.BAD_REQUEST);
    verifyCallToGetQuestionnaireIdNotCalled();
  }

  private void verifyEqLaunchJwe(String questionnaireId, boolean individual, FormType formType)
      throws Exception {
    Mockito.verify(eqLaunchService).getEqLaunchJwe(eqLaunchDataCaptor.capture());
    EqLaunchData eqLaunchData = eqLaunchDataCaptor.getValue();

    assertEquals(Language.ENGLISH, eqLaunchData.getLanguage());
    assertEquals(Source.CONTACT_CENTRE_API, eqLaunchData.getSource());
    assertEquals(Channel.CC, eqLaunchData.getChannel());
    assertEquals(AN_AGENT_ID, eqLaunchData.getUserId());
    assertEquals(questionnaireId, eqLaunchData.getQuestionnaireId());
    assertEquals(formType.name(), eqLaunchData.getFormType());
    assertNull(eqLaunchData.getAccountServiceLogoutUrl());
    assertNull(eqLaunchData.getAccountServiceUrl());
    if (individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, eqLaunchData.getCaseContainer().getId());
    } else {
      assertEquals(UUID_0, eqLaunchData.getCaseContainer().getId());
    }
  }

  private void verifySurveyLaunchedEventPublished(
      boolean individual, UUID caseId, String questionnaireId) {
    SurveyLaunchResponse payloadSent =
        verifyEventSent(TopicType.SURVEY_LAUNCH, SurveyLaunchResponse.class);
    if (individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, payloadSent.getCaseId());
    } else {
      assertEquals(caseId, payloadSent.getCaseId());
    }
    assertEquals(questionnaireId, payloadSent.getQuestionnaireId());
    assertEquals(AN_AGENT_ID, payloadSent.getAgentId());
  }

  private void verifyCorrectIndividualCaseId(boolean individual) {
    // Verify call to RM to get qid is using the correct individual case id
    Mockito.verify(caseServiceClient)
        .getSingleUseQuestionnaireId(any(), eq(individual), individualCaseIdCaptor.capture());
    if (individual) {
      assertNotEquals(UUID_0, individualCaseIdCaptor.getValue()); // expecting newly allocated uuid
    } else {
      assertNull(individualCaseIdCaptor.getValue());
    }
  }

  private void verifyCallToGetQuestionnaireIdNotCalled() {
    verify(caseServiceClient, never()).getSingleUseQuestionnaireId(any(), anyBoolean(), any());
  }

  private void doLaunchTest(boolean individual) throws Exception {
    mockGetCaseById(A_REGION.name());
    doLaunchTest(individual, FormType.H);
  }

  private void doLaunchTest(boolean individual, FormType formType) throws Exception {

    // Fake RM response for creating questionnaire ID
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(A_QUESTIONNAIRE_ID);
    newQuestionnaireIdDto.setFormType(formType.name());
    Mockito.lenient()
        .when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    // Mock out building of launch payload
    Mockito.lenient()
        .when(eqLaunchService.getEqLaunchJwe(any(EqLaunchData.class)))
        .thenReturn("simulated-encrypted-payload");

    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);
    launchRequestDTO.setIndividual(individual);

    // Invoke method under test, and check returned url
    String launchUrl = target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
    assertEquals(
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + "simulated-encrypted-payload",
        launchUrl);

    verifyCorrectIndividualCaseId(individual);
    verifyEqLaunchJwe(A_QUESTIONNAIRE_ID, individual, formType);
    verifySurveyLaunchedEventPublished(individual, UUID_0, A_QUESTIONNAIRE_ID);
  }
}

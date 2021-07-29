package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_UAC;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.NI_LAUNCH_ERR_MSG;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UNIT_LAUNCH_ERR_MSG;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getUACForCaseId(UUID, UACRequestDTO) getUACForCaseId}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetUACTest extends CaseServiceImplTestBase {

  @Test
  public void testGetUACCECase() throws Exception {
    mockGetCaseById("CE", "E", A_REGION.name());
    doGetUACTest(false, FormType.C);
  }

  @Test
  public void testGetUACCECaseForIndividual() throws Exception {
    doGetUACTest("CE", true);
  }

  @Test
  public void testGetUACHHCase() throws Exception {
    doGetUACTest("HH", false);
  }

  @Test
  public void testGetUACHHCaseForIndividual() throws Exception {
    doGetUACTest("HH", true);
  }

  @Test
  public void testGetUACSPGCase() throws Exception {
    doGetUACTest("SPG", false);
  }

  @Test
  public void testGetUACSPGCaseForIndividual() throws Exception {
    doGetUACTest("SPG", true);
  }

  @Test
  public void testGetUACHICase() {
    Exception e = assertThrows(Exception.class, () -> doGetUACTest("HI", false));
    assertTrue(e.getMessage().contains("must be SPG, CE or HH"), e.toString());
  }

  @Test
  public void testGetUAC_caseServiceCaseNotFoundException() throws Exception {
    Mockito.doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND))
        .when(caseDataClient)
        .getCaseById(UUID_0, false);
    assertThrows(CTPException.class, () -> target.getUACForCaseId(UUID_0, new UACRequestDTO()));
  }

  @Test
  public void testGetUAC_caseServiceCaseRequestResponseStatusException() throws Exception {
    Mockito.doThrow(new IllegalArgumentException()).when(caseDataClient).getCaseById(UUID_0, false);
    assertThrows(
        IllegalArgumentException.class, () -> target.getUACForCaseId(UUID_0, new UACRequestDTO()));
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionBadRequestCause()
      throws Exception {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Bad Request",
            new HttpClientErrorException(HttpStatus.BAD_REQUEST)),
        true);
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionOtherCause() throws Exception {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Other",
            new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT)),
        false);
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionNoCause() throws Exception {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error"),
        false);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionE() throws Exception {
    mockGetCaseById("CE", "U", "E");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionW() throws Exception {
    mockGetCaseById("CE", "U", "W");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionN() throws Exception {
    mockGetCaseById("CE", "U", "N");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromEstabRegionN() throws Exception {
    mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(NI_LAUNCH_ERR_MSG);
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(String expectedMsg) {
    CTPException e = assertThrows(CTPException.class, () -> doGetUACTest(false, FormType.C));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertTrue(e.getMessage().contains(expectedMsg), e.toString());
  }

  private void doGetUACTest(String caseType, boolean individual) throws Exception {
    mockGetCaseById(caseType, "U", A_REGION.name());
    doGetUACTest(individual, FormType.H);
  }

  private void doGetUACTest(boolean individual, FormType formType) throws Exception {

    // Fake RM response for creating questionnaire ID
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(A_QUESTIONNAIRE_ID);
    newQuestionnaireIdDto.setUac(A_UAC);
    newQuestionnaireIdDto.setFormType(formType.name());
    Mockito.lenient()
        .when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    UACRequestDTO requestsFromCCSvc =
        UACRequestDTO.builder().adLocationId(AN_AGENT_ID).individual(individual).build();

    long timeBeforeInvocation = System.currentTimeMillis();
    UACResponseDTO uac = target.getUACForCaseId(UUID_0, requestsFromCCSvc);
    long timeAfterInvocation = System.currentTimeMillis();

    assertEquals(A_UAC, uac.getUac());
    assertEquals(A_QUESTIONNAIRE_ID, uac.getId());
    verifyTimeInExpectedRange(timeBeforeInvocation, timeAfterInvocation, uac.getDateTime());
  }
}

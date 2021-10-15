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
  public void testGetUACHHCase() throws Exception {
    doGetUACTest(false);
  }

  @Test
  public void testGetUACHHCaseForIndividual() throws Exception {
    doGetUACTest(true);
  }

  @Test
  public void testGetUAC_caseServiceCaseNotFoundException() throws Exception {
    mockGetCaseById(UUID_0, new CTPException(Fault.RESOURCE_NOT_FOUND));
    assertThrows(CTPException.class, () -> target.getUACForCaseId(UUID_0, new UACRequestDTO()));
  }

  @Test
  public void testGetUAC_caseServiceCaseRequestResponseStatusException() throws Exception {
    mockGetCaseById(UUID_0, new IllegalArgumentException());
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

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(String expectedMsg) {
    CTPException e = assertThrows(CTPException.class, () -> doGetUACTest(false, FormType.C));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertTrue(e.getMessage().contains(expectedMsg), e.toString());
  }

  private void doGetUACTest(boolean individual) throws Exception {
    mockGetCaseById(A_REGION.name());
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

package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

// This used to have a lot of tests in Census but had to be deleted since the depended
// heavily on EstabType and CaseType.

@ExtendWith(MockitoExtension.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private Case caze;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
    requestDTO = FixtureHelper.loadClassFixtures(ModifyCaseRequestDTO[].class).get(0);
    caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    lenient().when(appConfig.getSurveyName()).thenReturn("CENSUS");
  }

  private void verifyRejectIncompatible(EstabType estabType, CaseType caseType) {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertTrue(e.getMessage().contains("is not compatible with caseType of"));
  }

  @Test
  public void shouldRejectIncompatibleCaseTypeAndEstabType() throws Exception {
    verifyRejectIncompatible(EstabType.APPROVED_PREMISES, CaseType.HH);
    verifyRejectIncompatible(EstabType.RESIDENTIAL_BOAT, CaseType.CE);
    verifyDbCaseCall(0);
  }

  private void verifyAcceptCompatible(EstabType estabType, CaseType caseType) throws Exception {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CaseResponseDTO response = target.modifyCase(requestDTO);
    assertNotNull(response);
  }

  private void verifyDbCaseCall(int times) throws CTPException {
    verify(caseDataClient, times(times)).getCaseById(any());
  }

  private void mockDbHasCase() throws Exception {
    mockGetCaseById(CASE_ID_0, caze);
  }

  private void mockDbCannotFindCase() throws Exception {
    mockGetCaseById(CASE_ID_0, new CTPException(Fault.RESOURCE_NOT_FOUND));
  }

  @Test
  public void shouldAcceptCompatibleCaseTypeAndEstabType() throws Exception {
    mockDbHasCase();
    verifyAcceptCompatible(EstabType.APPROVED_PREMISES, CaseType.CE);
    verifyAcceptCompatible(EstabType.RESIDENTIAL_BOAT, CaseType.HH);
    verifyDbCaseCall(2);
  }

  @Test
  public void shouldReturnNotFoundWhenCaseDoesNotExist() throws Exception {
    mockDbCannotFindCase();
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
  }
}

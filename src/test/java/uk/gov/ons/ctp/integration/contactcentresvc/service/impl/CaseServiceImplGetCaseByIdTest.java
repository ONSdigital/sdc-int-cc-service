package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;

/** Unit Test {@link CaseService#getCaseById(UUID, CaseQueryRequestDTO) getCaseById}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByIdTest extends CaseServiceImplTestBase {

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
  }

  @Test
  public void shouldGetCaseByCaseId_withNoInteractionHistory() throws Exception {
    // Build results to be returned from search
    Case caze = casesFromDb().get(0);
    CaseDTO expectedCaseResult;

    mockGetCaseById(CASE_ID_0, caze);
    expectedCaseResult = createExpectedCaseDTO(caze);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    CaseDTO results = target.getCaseById(CASE_ID_0, requestParams);

    verifyCase(results, expectedCaseResult);
    assertEquals(0, results.getInteractions().size());
  }

  @Test
  public void shouldGetCaseByCaseId_withInteractionHistory() throws Exception {
    // Build results to be returned from search
    Case caze = casesFromDb().get(0);
    CaseDTO expectedCaseResult;

    mockGetCaseById(CASE_ID_0, caze);
    expectedCaseResult = createExpectedCaseDTO(caze);

    mockRmGetCaseDTO(CASE_ID_0);
    mockCaseInteractionRepoFindByCaseId(CASE_ID_0);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    CaseDTO results = target.getCaseById(CASE_ID_0, requestParams);

    verifyCase(results, expectedCaseResult);

    List<CaseInteractionDTO> expectedInteractions =
        FixtureHelper.loadPackageFixtures(CaseInteractionDTO[].class);
    verifyInteractions(expectedInteractions, results.getInteractions());
  }

  @Test
  public void testCaseNotFound() throws Exception {
    doGetCaseByIdNotFound(CASE_ID_0);
  }

  @Test
  public void testHandleErrorFromDatabase() throws Exception {
    doGetCaseByIdGetsError(CASE_ID_0);
  }

  @Test
  public void testHandleErrorFromRM() throws Exception {
    // Mock db case lookup
    Case caze = casesFromDb().get(0);
    mockGetCaseById(CASE_ID_0, caze);

    // Fetching case from RM is going to fail
    mockRmGetCaseDTOFailure(CASE_ID_0, HttpStatus.NOT_FOUND);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    try {
      target.getCaseById(CASE_ID_0, requestParams);
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage().contains("when calling RM"), e.getMessage());
    }
  }

  @Test
  public void testCanLaunchableCase() throws Exception {
    checkCanLaunchFlag(1970, 2189, 999999, true);
  }

  // SOCINT-432
  // @Test
  // public void testNotLaunchableAfterLastWave() throws Exception {
  //  checkCanLaunchFlag(2000, 2001, 122, false);
  // }
  //
  // @Test
  // public void testNotLaunchableBeforeFirstWave() throws Exception {
  //  checkCanLaunchFlag(2088, 2089, 100, false);
  // }

  private void checkCanLaunchFlag(
      int startYear, int endYear, int waveLength, boolean expectedCanLaunch)
      throws Exception, CTPException {
    // Build results to be returned from search
    Case caze = casesFromDb().get(0);
    caze.getCollectionExercise().setStartDate(LocalDateTime.of(startYear, 1, 1, 1, 1, 1));
    caze.getCollectionExercise().setEndDate(LocalDateTime.of(endYear, 1, 1, 1, 1, 1));
    caze.getCollectionExercise().setWaveLength(waveLength);

    mockGetCaseById(CASE_ID_0, caze);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    CaseDTO results = target.getCaseById(CASE_ID_0, requestParams);

    assertEquals(expectedCanLaunch, results.isCanLaunch());
  }

  private List<Case> casesFromDb() {
    return FixtureHelper.loadPackageFixtures(Case[].class);
  }

  private void doGetCaseByIdNotFound(UUID caseId) throws Exception {
    mockGetCaseById(caseId, new CTPException(Fault.RESOURCE_NOT_FOUND, "Case Id Not Found:"));

    Fault fault = null;
    String message = null;
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    // Run the request
    try {
      target.getCaseById(caseId, requestParams);
    } catch (CTPException e) {
      fault = e.getFault();
      message = e.getMessage();
    }

    assertEquals(Fault.RESOURCE_NOT_FOUND, fault);
    assertTrue(message.contains("Case Id Not Found:"));
  }

  private void doGetCaseByIdGetsError(UUID caseId) throws Exception {
    mockGetCaseById(
        caseId, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)); // RM problems.

    HttpStatus status = null;
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    // Run the request
    try {
      target.getCaseById(caseId, requestParams);
    } catch (ResponseStatusException e) {
      status = e.getStatus();
    }

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);
  }
}

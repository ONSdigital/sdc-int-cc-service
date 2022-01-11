package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getCaseById(UUID, CaseQueryRequestDTO) getCaseById}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByIdTest extends CaseServiceImplTestBase {

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
  }

  private static Stream<Arguments> dataForGetCaseByCaseIdSuccess() {
    return Stream.of(arguments(true), arguments(false));
  }

  @ParameterizedTest
  @MethodSource("dataForGetCaseByCaseIdSuccess")
  public void shouldGetCaseByCaseId(boolean caseEvents) throws Exception {
    // Build results to be returned from search
    Case caze = casesFromDb().get(0);
    CaseDTO expectedCaseResult;

    mockGetCaseById(CASE_ID_0, caze);
    expectedCaseResult = createExpectedCaseDTO(caze);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(CASE_ID_0, requestParams);

    verifyCase(results, expectedCaseResult);
  }

  @Test
  public void testCaseNotFound() throws Exception {
    doGetCaseByIdNotFound(CASE_ID_0);
  }

  @Test
  public void testHandleErrorFromRM() throws Exception {
    doGetCaseByIdGetsError(CASE_ID_0);
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

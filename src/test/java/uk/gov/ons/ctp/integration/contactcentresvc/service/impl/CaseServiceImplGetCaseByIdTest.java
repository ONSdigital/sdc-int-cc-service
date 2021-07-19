package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getCaseById(UUID, CaseQueryRequestDTO) getCaseById}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByIdTest extends CaseServiceImplTestBase {
  private static final boolean CASE_EVENTS_TRUE = true;
  private static final boolean CASE_EVENTS_FALSE = false;

  private static final String AN_ESTAB_UPRN = "334111111111";

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
  }

  private static Stream<Arguments> dataForGetCaseByCaseIdSuccess() {
    return Stream.of(
        arguments(CaseType.HH, CASE_EVENTS_TRUE),
        arguments(CaseType.HH, CASE_EVENTS_FALSE),
        arguments(CaseType.CE, CASE_EVENTS_TRUE),
        arguments(CaseType.CE, CASE_EVENTS_FALSE),
        arguments(CaseType.SPG, CASE_EVENTS_FALSE),
        arguments(CaseType.SPG, CASE_EVENTS_FALSE),
        arguments(CaseType.SPG, CASE_EVENTS_TRUE));
  }

  @ParameterizedTest
  @MethodSource("dataForGetCaseByCaseIdSuccess")
  public void shouldGetCaseByCaseId(CaseType caseType, boolean caseEvents) throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    CaseDTO expectedCaseResult;

    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);
    expectedCaseResult = createExpectedCaseDTO(caseFromCaseService, caseEvents);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    verifyCase(results, expectedCaseResult, caseEvents);

    assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());
  }

  @Test
  public void shouldAdaptNullEstabTypeToHousehold() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setEstabType(null);
    caseFromCaseService.setCaseType(CaseType.HH.name());
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);
    assertEquals(EstabType.HOUSEHOLD, results.getEstabType());
  }

  @Test
  public void shouldAdaptNullEstabTypeToOther() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(CaseType.CE.name());
    caseFromCaseService.setEstabType(null);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);
    assertEquals(EstabType.OTHER, results.getEstabType());
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseId() throws CTPException {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_1), any())).thenReturn(caseFromCaseService);

    CaseDTO results = target.getCaseById(UUID_1, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  @Test
  public void testGetCaseByCaseId_householdIndividualCase() throws CTPException {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseById(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseById(UUID_0, new CaseQueryRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is not suitable", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void testCaseNotFound() throws CTPException {
    doGetCaseByIdNotFound(UUID_0);
  }

  @Test
  public void testHandleErrorFromRM() throws CTPException {
    doGetCaseByIdGetsError(UUID_0);
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  private void doGetCaseByIdNotFound(UUID caseId) throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM

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

  private void doGetCaseByIdGetsError(UUID caseId) throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)); // RM problems

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

package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseBySampleAttribute(String, String, CaseQueryRequestDTO)
 * getCaseBySampleAttribute}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseBySampleAttributeTest extends CaseServiceImplTestBase {
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";

  List<Case> casesFromDb;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();

    lenient().when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);

    casesFromDb = FixtureHelper.loadPackageFixtures(Case[].class);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetails() throws Exception {
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyDbCase(result, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetails() throws Exception {
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    verifyDbCase(result, 0);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse() throws Exception {
    mockNothingInDb();
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_caseSvcUncheckedException() throws Exception {
    doThrow(new IllegalArgumentException())
        .when(caseDataClient)
        .getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            target.getCaseBySampleAttribute(
                "uprn", String.valueOf(UPRN.getValue()), new CaseQueryRequestDTO(false)));
  }

  @Test
  public void testGetCaseByUprn() throws Exception {
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyDbCase(result, 0);
  }

  // ---- helpers methods below ---

  private void mockCasesFromDb() throws Exception {
    when(caseDataClient.getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue())))
        .thenReturn(casesFromDb);
  }

  private void mockNothingInDb() throws Exception {
    doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND))
        .when(caseDataClient)
        .getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue()));
  }

  private void verifyCallToGetCasesFromDb() throws Exception {
    verify(caseDataClient).getCaseBySampleAttribute(eq("uprn"), anyString());
  }

  private void verifyDbCase(CaseDTO results, int dataIndex) throws Exception {
    CaseDTO expectedCaseResult = createExpectedCaseDTO(casesFromDb.get(dataIndex));
    verifyCase(results, expectedCaseResult);
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results =
        target.getCaseBySampleAttribute(
            "uprn", String.valueOf(UPRN.getValue()), new CaseQueryRequestDTO(caseEvents));
    return results.size() >= 1 ? results.get(0) : null;
  }
}

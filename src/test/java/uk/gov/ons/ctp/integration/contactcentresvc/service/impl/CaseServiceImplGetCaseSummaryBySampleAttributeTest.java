package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseSummaryDTO;

/**
 * Unit Test {@link CaseService#getCaseBySampleAttribute(String, String, CaseQueryRequestDTO)
 * getCaseBySampleAttribute}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseSummaryBySampleAttributeTest extends CaseServiceImplTestBase {
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  List<Case> casesFromDb;

  @BeforeEach
  public void setup() {
    casesFromDb = FixtureHelper.loadPackageFixtures(Case[].class);
  }

  @Test
  public void testGetCaseByUprn_caseFound() throws Exception {
    mockCasesFromDb();
    CaseSummaryDTO result = getCasesByUprn();
    verifyDbCase(result, 0);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse() throws Exception {
    mockNothingInDb();
    assertNull(getCasesByUprn());
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_caseSvcUncheckedException() throws Exception {
    doThrow(new IllegalArgumentException())
        .when(caseDataClient)
        .getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue()));

    assertThrows(
        IllegalArgumentException.class,
        () -> target.getCaseSummaryBySampleAttribute("uprn", String.valueOf(UPRN.getValue())));
  }

  // ---- helpers methods below ---

  private void mockCasesFromDb() {
    when(caseDataClient.getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue())))
        .thenReturn(casesFromDb);
  }

  private void mockNothingInDb() throws CTPException {
    when(caseDataClient.getCaseBySampleAttribute("uprn", String.valueOf(UPRN.getValue())))
        .thenReturn(new ArrayList<Case>());
  }

  private void verifyCallToGetCasesFromDb() {
    verify(caseDataClient).getCaseBySampleAttribute(eq("uprn"), anyString());
  }

  private void verifyDbCase(CaseSummaryDTO results, int dataIndex) throws Exception {
    CaseSummaryDTO expectedCaseResult = createExpectedCaseSummaryDTO(casesFromDb.get(dataIndex));
    assertEquals(expectedCaseResult, results);
  }

  private CaseSummaryDTO getCasesByUprn() throws CTPException {
    List<CaseSummaryDTO> results =
        target.getCaseSummaryBySampleAttribute("uprn", String.valueOf(UPRN.getValue()));
    return results.size() >= 1 ? results.get(0) : null;
  }
}

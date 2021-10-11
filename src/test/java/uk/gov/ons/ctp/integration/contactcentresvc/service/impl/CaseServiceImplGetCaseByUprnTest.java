package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByUPRN(UniquePropertyReferenceNumber, CaseQueryRequestDTO)
 * getCaseByUPRN}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByUprnTest extends CaseServiceImplTestBase {
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";

  List<CaseContainerDTO> casesFromDb;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();

    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
    lenient().when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);

    casesFromDb = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyDbCase(result, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    verifyDbCase(result, 0);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_HH() throws Exception {
    mockNothingInDb();
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_caseSvcUncheckedException() throws Exception {
    doThrow(new IllegalArgumentException()).when(caseDataClient).getCaseByUprn(eq(UPRN.getValue()));

    assertThrows(
        IllegalArgumentException.class,
        () -> target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false)));
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    casesFromDb.get(0).setCaseType("HH");
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyDbCase(result, 0);
  }

  // ---- helpers methods below ---

  private void mockCasesFromDb() throws Exception {
    when(caseDataClient.getCaseByUprn(eq(UPRN.getValue()))).thenReturn(casesFromDb);
  }

  private void mockNothingInDb() throws Exception {
    doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND))
        .when(caseDataClient)
        .getCaseByUprn(eq(UPRN.getValue()));
  }

  private void verifyCallToGetCasesFromDb() throws Exception {
    verify(caseDataClient).getCaseByUprn(any(Long.class));
  }

  private void verifyDbCase(CaseDTO results, int dataIndex) throws Exception {
    CaseDTO expectedCaseResult = createExpectedCaseDTO(casesFromDb.get(dataIndex));
    verifyCase(results, expectedCaseResult);
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    return results.size() > 1 ? results.get(0) : null;
  }
}

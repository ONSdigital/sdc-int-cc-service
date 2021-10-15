package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByCaseReference(long, CaseQueryRequestDTO)
 * getCaseByCaseReference}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByCaseRefTest extends CaseServiceImplTestBase {
  private static final boolean CASE_EVENTS_TRUE = true;
  private static final boolean CASE_EVENTS_FALSE = false;

  private static final long VALID_CASE_REF = 882_345_440L;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CASE_EVENTS_TRUE);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CASE_EVENTS_FALSE);
  }

  private void rejectNonLuhn(long caseRef) {
    CaseQueryRequestDTO dto = new CaseQueryRequestDTO(false);
    CTPException e =
        assertThrows(CTPException.class, () -> target.getCaseByCaseReference(caseRef, dto));
    assertEquals("Invalid Case Reference", e.getMessage());
  }

  @Test
  public void shouldRejectGetByNonLuhnCaseReference() {
    rejectNonLuhn(123);
    rejectNonLuhn(1231);
    rejectNonLuhn(100000000);
    rejectNonLuhn(999999999);
  }

  private void acceptLuhn(long caseRef) throws Exception {
    mockGetCaseByRef(casesFromDatabase().get(0));
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    target.getCaseByCaseReference(caseRef, requestParams);
  }

  @Test
  public void shouldAcceptLuhnCompliantGetByCaseReference() throws Exception {
    acceptLuhn(1230);
    acceptLuhn(VALID_CASE_REF);
    acceptLuhn(100000009);
    acceptLuhn(999999998);
  }

  @Test
  public void shouldReport404ForBlacklistedUPRN() throws Exception {
    Case caseFromDb = casesFromDatabase().get(0);
    mockGetCaseByRef(caseFromDb);

    when(blacklistedUPRNBean.isUPRNBlacklisted(any())).thenReturn(true);

    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    try {
      target.getCaseByCaseReference(VALID_CASE_REF, requestParams);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
      assertTrue(e.getMessage().contains("IVR restricted"), e.getMessage());
    }
  }

  private void doTestGetCaseByCaseRef(boolean caseEvents) throws Exception {
    // Build results to be returned from search
    Case caseFromDb = casesFromDatabase().get(0);
    mockGetCaseByRef(caseFromDb);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseByCaseReference(VALID_CASE_REF, requestParams);
    CaseDTO expectedCaseResult = createExpectedCaseDTO(caseFromDb);
    verifyCase(results, expectedCaseResult);
  }

  private void mockGetCaseByRef(Case result) throws Exception {
    Mockito.when(caseDataClient.getCaseByCaseRef(any())).thenReturn(result);
  }

  private List<Case> casesFromDatabase() {
    return FixtureHelper.loadPackageFixtures(Case[].class);
  }
}

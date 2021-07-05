package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
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
public class CaseServiceImplGetCcsCaseByPostcodeTest extends CaseServiceImplTestBase {

  private static final UUID CASE_ID = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private static final String POSTCODE_IN_CCS_SET = "GW12 AAA";
  private static final String POSTCODE_NOT_IN_CCS_SET = "GW12 AAC";
  List<CaseContainerDTO> casesFromRm;

  @BeforeEach
  public void setup() throws IOException {
    casesFromRm = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  @Test
  public void testGetCcsCaseByPostcode_caseSvcRestClientException() throws Exception {
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);
    doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET));

    assertThrows(
        ResponseStatusException.class, () -> target.getCCSCaseByPostcode(POSTCODE_IN_CCS_SET));
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeInRMAndInCCSPostcodes() throws CTPException {
    casesFromRm.get(1).setPostcode(POSTCODE_IN_CCS_SET);
    when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);
    CaseDTO result = getCasesByPostcode(POSTCODE_IN_CCS_SET);
    assertEquals(CASE_ID, result.getId());
  }

  @Test
  public void testGetCcsCaseByPostcode_returnSupportedCaseTypes() throws CTPException {
    casesFromRm.get(0).setCaseType("HH");
    casesFromRm.get(1).setCaseType("CE");
    casesFromRm.get(2).setCaseType("SPG");
    when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);

    List<CaseDTO> results = target.getCCSCaseByPostcode(POSTCODE_IN_CCS_SET);

    assertEquals("HH", results.get(0).getCaseType());
    assertEquals("CE", results.get(1).getCaseType());
    assertEquals("SPG", results.get(2).getCaseType());
    assertEquals(3, results.size());
  }

  @Test
  public void testGetCcsCaseByPostcode_noUnspportedCaseTypes() throws CTPException {
    casesFromRm.get(0).setCaseType("NR");
    casesFromRm.get(1).setCaseType("TBD");
    casesFromRm.get(2).setCaseType("HH");
    when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);

    List<CaseDTO> results = target.getCCSCaseByPostcode(POSTCODE_IN_CCS_SET);

    assertEquals("HH", results.get(0).getCaseType());
    assertEquals(1, results.size());
  }

  @Test
  public void testGetCcsCaseByPostcode_fiterOutNullCaseTypes() throws CTPException {
    casesFromRm.get(0).setCaseType("");
    casesFromRm.get(1).setCaseType(null);
    casesFromRm.get(2).setCaseType("CE");
    when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);

    List<CaseDTO> results = target.getCCSCaseByPostcode(POSTCODE_IN_CCS_SET);

    assertEquals("CE", results.get(0).getCaseType());
    assertEquals(1, results.size());
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeNotInRMAndInCCSPostcodes() throws CTPException {
    when(ccsPostcodesBean.isInCCSPostcodes(POSTCODE_IN_CCS_SET)).thenReturn(true);
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET));
    try {
      getCasesByPostcode(POSTCODE_IN_CCS_SET);
      fail();
    } catch (ResponseStatusException notFound) {
      assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
    }
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeNotInCCSPostcodes() throws CTPException {
    try {
      getCasesByPostcode(POSTCODE_NOT_IN_CCS_SET);
      fail();
    } catch (CTPException badRequest) {
      assertEquals(Fault.BAD_REQUEST, badRequest.getFault());
    }
  }

  private CaseDTO getCasesByPostcode(String postcode) throws CTPException {
    List<CaseDTO> results = target.getCCSCaseByPostcode(postcode);
    return results.get(0);
  }
}

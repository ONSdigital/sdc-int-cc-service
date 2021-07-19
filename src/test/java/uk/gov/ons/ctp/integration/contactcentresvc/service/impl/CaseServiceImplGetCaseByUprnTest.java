package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByUPRN(UniquePropertyReferenceNumber, CaseQueryRequestDTO)
 * getCaseByUPRN}.
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplGetCaseByUprnTest extends CaseServiceImplTestBase {
  private static final String AN_ESTAB_UPRN = "334111111111";
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  List<CaseContainerDTO> casesFromRm;
  private AddressIndexAddressCompositeDTO addressFromAI;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();

    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
    lenient().when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    lenient().when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);

    casesFromRm = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    addressFromAI = FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void shouldHandleNullEstabTypeFromRmAndConvertToHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    casesFromRm.get(0).setEstabType(null);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    assertEquals(EstabType.HOUSEHOLD, result.getEstabType());
  }

  @Test
  public void shouldHandleNullEstabTypeFromRmAndConvertToOTHER() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.CE.name());
    casesFromRm.get(0).setEstabType(null);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    assertEquals(EstabType.OTHER, result.getEstabType());
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeCE() throws Exception {
    casesFromRm.get(1).setCaseType(CaseType.CE.name());
    setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
    setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    verifyRmCase(result, false, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeCE() throws Exception {
    casesFromRm.get(1).setCaseType(CaseType.CE.name());
    setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
    setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    verifyRmCase(result, false, 1);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet() throws Exception {

    casesFromRm.get(0).setCaseType("HI");
    casesFromRm.get(1).setCaseType("HI");
    casesFromRm.get(2).setCaseType("HI");

    mockCasesFromRm();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_HH() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForNAAddress() throws Exception {
    addressFromAI.setCensusAddressType("NA");
    addressFromAI.setCensusEstabType("X");
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  private void verifyCaseNotFound(String estabType) throws Exception {
    addressFromAI.setCensusEstabType(estabType);

    mockNothingInRm();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_SPG() throws Exception {
    verifyCaseNotFound("marina");
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_CE() throws Exception {
    addressFromAI.setCensusAddressType(AddressType.CE.name());

    String estabType = "CARE HOME";
    addressFromAI.setCensusEstabType(estabType);

    mockNothingInRm();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NA() throws Exception {
    verifyCaseNotFound("NA");
  }

  // CR-1823 - when we have AIMS mismatching addressType. we need addressLevel to be based on
  // CaseType, not EstabType.
  // Testing: mismatching estabType CE with HH addressType (case type)
  @Test
  public void shouldUseCaseTypeToDetermineAddressLevel_U() throws Exception {
    String estabType = EstabType.STAFF_ACCOMMODATION.getCode();
    addressFromAI.setCensusEstabType(estabType);
    assertEquals("HH", addressFromAI.getCensusAddressType());

    mockNothingInRm();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  // CR-1823 - when we have AIMS mismatching addressType. we need addressLevel to be based on
  // CaseType, not EstabType.
  // Testing: mismatching estabType household with CE addressType (case type)
  @Test
  public void shouldUseCaseTypeToDetermineAddressLevel_E() throws Exception {
    String addressType = AddressType.CE.name();
    addressFromAI.setCensusAddressType(addressType);
    assertEquals(EstabType.HOUSEHOLD, EstabType.forCode(addressFromAI.getCensusEstabType()));

    mockNothingInRm();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcRestClientException() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    assertThrows(
        ResponseStatusException.class,
        () -> target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false)));
  }

  @Test
  public void testGetCaseByUprn_mixedCaseTypes() throws Exception {
    casesFromRm.get(0).setCaseType("HI"); // Household Individual case
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_caseSPG() throws Exception {
    casesFromRm.get(0).setCaseType("SPG");
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    casesFromRm.get(0).setCaseType("HH");
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void shouldGetSecureEstablishmentByUprn() throws Exception {
    setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
    setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    assertTrue(result.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), result.getEstabUprn());
  }

  @Test
  public void shouldGetLatestFromRm() throws Exception {
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 23, 0, 0)));
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(UUID_1, result.getId());
  }

  @Test
  public void shouldGetOtherLatestFromRm() throws Exception {
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 4, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2019, 12, 12, 0, 0)));
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(UUID_0, result.getId());
  }

  // ---- helpers methods below ---

  private Date utcDate(LocalDateTime dateTime) {
    return Date.from(dateTime.toInstant(ZoneOffset.UTC));
  }

  private void setLastUpdated(CaseContainerDTO caze, int year, int month, int dayOfMonth) {
    LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0);
    caze.setLastUpdated(utcDate(dateTime));
  }

  private void mockCasesFromRm() {
    when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any())).thenReturn(casesFromRm);
  }

  private void mockNothingInRm() {
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
  }

  private void verifyCallToGetCasesFromRm() {
    verify(caseServiceClient).getCaseByUprn(any(Long.class), any(Boolean.class));
  }

  private void verifyRmCase(CaseDTO results, boolean caseEventsExpected, int dataIndex)
      throws Exception {
    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(casesFromRm.get(dataIndex), caseEventsExpected);

    verifyCase(results, expectedCaseResult, caseEventsExpected);
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertTrue(results.size() <= 1);
    return results.size() == 1 ? results.get(0) : null;
  }
}

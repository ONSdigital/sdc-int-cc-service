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
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
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

  List<CaseContainerDTO> casesFromDb;
  private AddressIndexAddressCompositeDTO addressFromAI;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();

    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
    lenient().when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    lenient().when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);

    casesFromDb = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    addressFromAI = FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void shouldHandleNullEstabTypeFromRmAndConvertToHH() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.HH.name());
    casesFromDb.get(0).setEstabType(null);
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    assertEquals(EstabType.HOUSEHOLD, result.getEstabType());
  }

  @Test
  public void shouldHandleNullEstabTypeFromRmAndConvertToOTHER() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.CE.name());
    casesFromDb.get(0).setEstabType(null);
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    assertEquals(EstabType.OTHER, result.getEstabType());
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeCE() throws Exception {
    casesFromDb.get(1).setCaseType(CaseType.CE.name());
    setLastUpdated(casesFromDb.get(0), 2020, 5, 14);
    setLastUpdated(casesFromDb.get(1), 2020, 5, 15);
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
    casesFromDb.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    verifyRmCase(result, false, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeCE() throws Exception {
    casesFromDb.get(1).setCaseType(CaseType.CE.name());
    setLastUpdated(casesFromDb.get(0), 2020, 5, 14);
    setLastUpdated(casesFromDb.get(1), 2020, 5, 15);
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    verifyRmCase(result, false, 1);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet() throws Exception {

    casesFromDb.get(0).setCaseType("HI");
    casesFromDb.get(1).setCaseType("HI");
    casesFromDb.get(2).setCaseType("HI");

    mockCasesFromDb();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_HH() throws Exception {
    mockNothingInDb();
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForNAAddress() throws Exception {
    addressFromAI.setCensusAddressType("NA");
    addressFromAI.setCensusEstabType("X");
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  private void verifyCaseNotFound(String estabType) throws Exception {
    addressFromAI.setCensusEstabType(estabType);

    mockNothingInDb();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
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

    mockNothingInDb();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
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

    mockNothingInDb();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  // CR-1823 - when we have AIMS mismatching addressType. we need addressLevel to be based on
  // CaseType, not EstabType.
  // Testing: mismatching estabType household with CE addressType (case type)
  @Test
  public void shouldUseCaseTypeToDetermineAddressLevel_E() throws Exception {
    String addressType = AddressType.CE.name();
    addressFromAI.setCensusAddressType(addressType);
    assertEquals(EstabType.HOUSEHOLD, EstabType.forCode(addressFromAI.getCensusEstabType()));

    mockNothingInDb();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromDb();
  }

  @Test
  public void testGetCaseByUprn_caseSvcUncheckedException() throws Exception {
    doThrow(new IllegalArgumentException())
        .when(caseDataClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    assertThrows(
        IllegalArgumentException.class,
        () -> target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false)));
  }

  @Test
  public void testGetCaseByUprn_mixedCaseTypes() throws Exception {
    casesFromDb.get(0).setCaseType("HI"); // Household Individual case
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_caseSPG() throws Exception {
    casesFromDb.get(0).setCaseType("SPG");
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    casesFromDb.get(0).setCaseType("HH");
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(true);
    verifyRmCase(result, true, 0);
  }

  @Test
  public void shouldGetSecureEstablishmentByUprn() throws Exception {
    setLastUpdated(casesFromDb.get(0), 2020, 5, 14);
    setLastUpdated(casesFromDb.get(1), 2020, 5, 15);
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    assertTrue(result.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), result.getEstabUprn());
  }

  @Test
  public void shouldGetLatestFromRm() throws Exception {
    casesFromDb.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
    casesFromDb.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 23, 0, 0)));
    mockCasesFromDb();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(UUID_1, result.getId());
  }

  @Test
  public void shouldGetOtherLatestFromRm() throws Exception {
    casesFromDb.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 4, 0, 0)));
    casesFromDb.get(1).setLastUpdated(utcDate(LocalDateTime.of(2019, 12, 12, 0, 0)));
    mockCasesFromDb();
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

  private void mockCasesFromDb() throws Exception {
    when(caseDataClient.getCaseByUprn(eq(UPRN.getValue()), any())).thenReturn(casesFromDb);
  }

  private void mockNothingInDb() throws Exception {
    doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND))
        .when(caseDataClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
  }

  private void verifyCallToGetCasesFromDb() throws Exception {
    verify(caseDataClient).getCaseByUprn(any(Long.class), any(Boolean.class));
  }

  private void verifyRmCase(CaseDTO results, boolean caseEventsExpected, int dataIndex)
      throws Exception {
    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(casesFromDb.get(dataIndex), caseEventsExpected);

    verifyCase(results, expectedCaseResult, caseEventsExpected);
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertTrue(results.size() <= 1);
    return results.size() == 1 ? results.get(0) : null;
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
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

  private static final UUID CACHED_CASE_ID_0 =
      UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private static final UUID CACHED_CASE_ID_1 =
      UUID.fromString("c46e5dd4-4b17-45ac-a034-0e514e8592c0");

  List<CaseContainerDTO> casesFromRm;
  List<CachedCase> casesFromCache;
  private AddressIndexAddressCompositeDTO addressFromAI;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();

    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
    lenient().when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    lenient().when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);

    casesFromRm = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    casesFromCache = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    addressFromAI = FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyNonCachedCase(result, true, 0);
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
    verifyNonCachedCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    verifyNonCachedCase(result, false, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeCE() throws Exception {
    casesFromRm.get(1).setCaseType(CaseType.CE.name());
    setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
    setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(false);
    verifyNonCachedCase(result, false, 1);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_noCachedCase()
      throws Exception {

    casesFromRm.get(0).setCaseType("HI");
    casesFromRm.get(1).setCaseType("HI");
    casesFromRm.get(2).setCaseType("HI");

    mockCasesFromRm();
    mockNothingInTheCache();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_HH() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    mockNothingInTheCache();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForNAAddress() throws Exception {
    addressFromAI.setCensusAddressType("NA");
    addressFromAI.setCensusEstabType("X");
    mockNothingInTheCache();
    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  private void verifyCaseNotFound(String estabType) throws Exception {
    addressFromAI.setCensusEstabType(estabType);

    mockNothingInRm();
    mockNothingInTheCache();

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
    mockNothingInTheCache();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_NA() throws Exception {
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
    mockNothingInTheCache();

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
    mockNothingInTheCache();

    assertNull(getCasesByUprn(false));
    verifyCallToGetCasesFromRm();
  }

  @Test
  public void shouldGetCachedCaseWithoutCaseEvents() throws Exception {
    mockNothingInRm();
    mockCachedCase();
    CaseDTO result = getCasesByUprn(false);
    verifyCachedCase(result, false);
    assertTrue(result.getCaseEvents().isEmpty());
  }

  @Test
  public void shouldGetCachedCaseWithCaseEvents() throws Exception {
    mockNothingInRm();
    mockCachedCase();
    CaseDTO result = getCasesByUprn(true);
    verifyCachedCase(result, true);
    assertFalse(result.getCaseEvents().isEmpty());
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
    verifyNonCachedCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_caseSPG() throws Exception {
    casesFromRm.get(0).setCaseType("SPG");
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyNonCachedCase(result, true, 0);
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    casesFromRm.get(0).setCaseType("HH");
    mockCasesFromRm();
    CaseDTO result = getCasesByUprn(true);
    verifyNonCachedCase(result, true, 0);
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

  // --- results from both RM and cache ...

  @Test
  public void shouldGetLatestFromCacheWhenResultsFromBothRmAndCache() throws Exception {
    mockCasesFromRm();
    mockCasesFromCache();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(CACHED_CASE_ID_1, result.getId());
  }

  @Test
  public void shouldGetLatestFromCacheWhenResultsFromBothRmAndCacheWithSmallTimeDifference()
      throws Exception {
    casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
    casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 4, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2019, 12, 12, 0, 0)));
    mockCasesFromRm();
    mockCasesFromCache();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(CACHED_CASE_ID_0, result.getId());
  }

  @Test
  public void shouldGetLatestFromRmWhenResultsFromBothRmAndCache() throws Exception {
    casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 2, 0, 0)));
    casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 3, 0, 0)));
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 23, 0, 0)));
    mockCasesFromRm();
    mockCasesFromCache();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(UUID_1, result.getId());
  }

  @Test
  public void shouldGetLatestFromRmWhenResultsFromBothRmAndCacheWithSmallTimeDifferences()
      throws Exception {
    casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 3, 0, 0)));
    casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
    mockCasesFromRm();
    mockCasesFromCache();
    CaseDTO result = getCasesByUprn(false);
    assertEquals(UUID_1, result.getId());
  }

  @Test
  public void shouldGetOtherLatestFromRmWhenResultsFromBothRmAndCache() throws Exception {
    casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 2, 0, 0)));
    casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 3, 0, 0)));
    casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 4, 0, 0)));
    casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2019, 12, 12, 0, 0)));
    mockCasesFromRm();
    mockCasesFromCache();
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

  private void mockCachedCase() throws Exception {
    when(dataRepo.readCachedCasesByUprn(UPRN)).thenReturn(List.of(casesFromCache.get(0)));
  }

  private void mockCasesFromCache() throws Exception {
    when(dataRepo.readCachedCasesByUprn(UPRN)).thenReturn(casesFromCache);
  }

  private void mockNothingInTheCache() throws Exception {
    when(dataRepo.readCachedCasesByUprn(UPRN)).thenReturn(new ArrayList<>());
  }

  private void verifyHasReadCachedCases() throws Exception {
    verify(dataRepo).readCachedCasesByUprn(any(UniquePropertyReferenceNumber.class));
  }

  private void verifyNonCachedCase(CaseDTO results, boolean caseEventsExpected, int dataIndex)
      throws Exception {
    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(casesFromRm.get(dataIndex), caseEventsExpected);

    verifyCase(results, expectedCaseResult, caseEventsExpected);
    verifyHasReadCachedCases();
  }

  private void verifyCachedCase(CaseDTO result, boolean caseEvents) throws Exception {
    CachedCase cachedCase = casesFromCache.get(0);

    CaseDTO expectedResult = mapperFacade.map(cachedCase, CaseDTO.class);
    expectedResult.setCaseType(CaseType.HH.name());
    expectedResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
    expectedResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));
    if (!caseEvents) {
      expectedResult.setCaseEvents(Collections.emptyList());
    }

    assertEquals(expectedResult, result);

    verifyCallToGetCasesFromRm();
    verifyHasReadCachedCases();
    verifyEventNotSent();
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertTrue(results.size() <= 1);
    return results.size() == 1 ? results.get(0) : null;
  }
}

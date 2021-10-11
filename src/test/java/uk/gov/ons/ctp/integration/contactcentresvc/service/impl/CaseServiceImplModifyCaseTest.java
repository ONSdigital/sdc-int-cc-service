package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

@ExtendWith(MockitoExtension.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private CaseContainerDTO caseContainerDTO;
  private CaseContainerDTO ccsSurveyTypeCaseContainerDTO;
  private CaseContainerDTO householdEstabTypeTypeCaseContainerDTO;

  @BeforeEach
  public void setup() {
    mockCaseEventWhiteList();
    requestDTO = FixtureHelper.loadClassFixtures(ModifyCaseRequestDTO[].class).get(0);
    caseContainerDTO = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(0);
    ccsSurveyTypeCaseContainerDTO =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(1);
    householdEstabTypeTypeCaseContainerDTO =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(2);
    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
    lenient().when(appConfig.getSurveyName()).thenReturn("CENSUS");
  }

  private void verifyRejectIncompatible(EstabType estabType, CaseType caseType) {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertTrue(e.getMessage().contains("is not compatible with caseType of"));
  }

  @Test
  public void shouldRejectIncompatibleCaseTypeAndEstabType() throws Exception {
    verifyRejectIncompatible(EstabType.APPROVED_PREMISES, CaseType.HH);
    verifyRejectIncompatible(EstabType.RESIDENTIAL_BOAT, CaseType.CE);
    verifyDbCaseCall(0);
  }

  private void verifyAcceptCompatible(EstabType estabType, CaseType caseType) throws Exception {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CaseDTO response = target.modifyCase(requestDTO);
    assertNotNull(response);
  }

  private void verifyDbCaseCall(int times) throws CTPException {
    verify(caseDataClient, times(times)).getCaseById(any(), eq(true));
  }

  private void mockDbHasCase() throws CTPException {
    when(caseDataClient.getCaseById(eq(UUID_0), eq(true))).thenReturn(caseContainerDTO);
  }

  private void mockDbCannotFindCase() throws CTPException {
    CTPException ex = new CTPException(Fault.RESOURCE_NOT_FOUND);
    when(caseDataClient.getCaseById(eq(UUID_0), eq(true))).thenThrow(ex);
  }

  @Test
  public void shouldReturnBadRequestWhenCCSSurveyTypeExists() throws Exception {
    when(caseDataClient.getCaseById(eq(UUID_0), eq(true)))
        .thenReturn(ccsSurveyTypeCaseContainerDTO);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals("Operation not permissible for a CCS Case", e.getMessage());
    verifyDbCaseCall(1);
  }

  @Test
  public void shouldReturnBadRequestWhenModifyHouseholdEstabTypeToOther() throws Exception {
    when(caseDataClient.getCaseById(eq(UUID_0), eq(true)))
        .thenReturn(householdEstabTypeTypeCaseContainerDTO);
    requestDTO.setEstabType(EstabType.OTHER);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals("The pre-existing Establishment Type cannot be changed to OTHER", e.getMessage());
    verifyDbCaseCall(1);
  }

  @Test
  public void shouldAcceptCompatibleCaseTypeAndEstabType() throws Exception {
    mockDbHasCase();
    verifyAcceptCompatible(EstabType.APPROVED_PREMISES, CaseType.CE);
    verifyAcceptCompatible(EstabType.RESIDENTIAL_BOAT, CaseType.HH);
    verifyDbCaseCall(2);
  }

  @Test
  public void shouldReturnNotFoundWhenCaseDoesNotExist() throws Exception {
    mockDbCannotFindCase();
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
  }

  @Test
  public void shouldRejectExistingHouseholdIndividualCase() throws Exception {
    caseContainerDTO.setCaseType(CaseType.HI.name());
    mockDbHasCase();
    ResponseStatusException e =
        assertThrows(ResponseStatusException.class, () -> target.modifyCase(requestDTO));
    assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    assertEquals("Case is not suitable", e.getReason());
  }

  private void verifyModifyAddress(
      CaseType requestCaseType, EstabType requestEstabType, String existingEstabType)
      throws Exception {
    verifyModifyAddress(requestCaseType, requestEstabType, existingEstabType, requestCaseType);
  }

  private void verifyModifyAddress(
      CaseType requestCaseType,
      EstabType requestEstabType,
      String existingEstabType,
      CaseType existingCaseType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    caseContainerDTO.setCaseType(existingCaseType.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
    verifyDbCaseCall(1);
    // event tests removed until modify events become clear
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingHouseHold() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.HOUSEHOLD.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingResidentialBoatHH() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.RESIDENTIAL_BOAT.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingEmbassySPG() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.EMBASSY.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingHouseHold() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.HOUSEHOLD.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingMilitarySfaHH() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.MILITARY_SFA.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingEmbassySPG() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.EMBASSY.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestCE_ExistingPrisonCE() throws Exception {
    verifyModifyAddress(CaseType.CE, EstabType.CARE_HOME, "prison");
  }

  private void verifyAddressTypeChanged(
      CaseType requestCaseType,
      EstabType requestEstabType,
      String existingEstabType,
      CaseType existingCaseType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    caseContainerDTO.setCaseType(existingCaseType.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
    verifyDbCaseCall(1);
    // verify event tests removed until events become clear
  }

  @Test
  public void shouldChangeAddressType_RequestOther_NonExistingOtherCE() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.OTHER, "Oblivion Sky Tower", CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestOther_OtherCE() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.OTHER, EstabType.OTHER.name(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingOtherCE() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.HOUSEHOLD, "Oblivion Sky Tower", CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(
        CaseType.HH, EstabType.HOUSEHOLD, EstabType.PRISON.getCode(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestSPG_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(
        CaseType.SPG, EstabType.EMBASSY, EstabType.PRISON.getCode(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingHousehold() throws Exception {
    verifyAddressTypeChanged(
        CaseType.CE, EstabType.CARE_HOME, EstabType.HOUSEHOLD.getCode(), CaseType.HH);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingEmbassySPG() throws Exception {
    verifyAddressTypeChanged(
        CaseType.CE, EstabType.CARE_HOME, EstabType.EMBASSY.getCode(), CaseType.SPG);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingOtherSPG() throws Exception {
    verifyAddressTypeChanged(CaseType.CE, EstabType.CARE_HOME, "Oblivion Sky Tower", CaseType.SPG);
  }

  @Test
  public void shouldChangeAddressType_RequestSPG_ExistingOtherNull() throws Exception {
    verifyAddressTypeChanged(CaseType.SPG, EstabType.EMBASSY, "Oblivion Sky Tower", CaseType.HH);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingOtherNull() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.HOUSEHOLD, "Oblivion Sky Tower", CaseType.SPG);
  }

  private void assertRejectNorthernIrelandChangeFromHouseholdToCE(String region) throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.CARE_HOME);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(region);
    mockDbHasCase();
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals(
        "All queries relating to Communal Establishments in Northern Ireland should be escalated to NISRA HQ",
        e.getMessage());
  }

  @Test
  public void shouldRejectNorthernIrelandChangeFromHouseholdToCE_regionN() throws Exception {
    assertRejectNorthernIrelandChangeFromHouseholdToCE("N");
  }

  @Test
  public void shouldRejectNorthernIrelandChangeFromHouseholdToCE_regionLowerCaseN()
      throws Exception {
    assertRejectNorthernIrelandChangeFromHouseholdToCE("n");
  }

  @Test
  public void shouldRejectNorthernIrelandChangeFromHouseholdToCE_regionWithTrailingChars()
      throws Exception {
    assertRejectNorthernIrelandChangeFromHouseholdToCE("N01234");
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotInNorthernIreland() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.CARE_HOME);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(Region.E.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotInNorthernIrelandAndNotRequestedCE()
      throws Exception {
    requestDTO.setCaseType(CaseType.SPG);
    requestDTO.setEstabType(EstabType.EMBASSY);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(Region.E.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotRequestedCE() throws Exception {
    requestDTO.setCaseType(CaseType.SPG);
    requestDTO.setEstabType(EstabType.EMBASSY);
    caseContainerDTO.setEstabType(EstabType.YOUTH_HOSTEL.getCode());
    caseContainerDTO.setCaseType(CaseType.CE.name());
    caseContainerDTO.setRegion(Region.N.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotExistingHouseHold() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.CARE_HOME);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    caseContainerDTO.setCaseType(CaseType.SPG.name());
    caseContainerDTO.setRegion(Region.N.name());
    mockDbHasCase();
    target.modifyCase(requestDTO);
  }

  private String uprnStr(UniquePropertyReferenceNumber uprn) {
    return uprn == null ? null : ("" + uprn.getValue());
  }

  private void verifyCaseResponse(CaseDTO response, boolean newCaseIdExpected) {
    assertNotNull(response);

    if (newCaseIdExpected) {
      assertNotEquals(caseContainerDTO.getId(), response.getId());
      assertNull(response.getCaseRef());
    } else {
      assertEquals(caseContainerDTO.getId(), response.getId());
      assertEquals(caseContainerDTO.getCaseRef(), response.getCaseRef());
    }

    assertEquals(requestDTO.getCaseType().name(), response.getCaseType());
    assertEquals(requestDTO.getAddressLine1(), response.getAddressLine1());
    assertEquals(requestDTO.getAddressLine2(), response.getAddressLine2());
    assertEquals(requestDTO.getAddressLine3(), response.getAddressLine3());
    assertEquals(caseContainerDTO.getTownName(), response.getTownName());
    assertEquals(caseContainerDTO.getPostcode(), response.getPostcode());
    assertEquals(caseContainerDTO.getRegion().substring(0, 1), response.getRegion());
    assertEquals(caseContainerDTO.getUprn(), uprnStr(response.getUprn()));
    assertEquals(ALL_DELIVERY_CHANNELS, response.getAllowedDeliveryChannels());
    assertTrue(response.getCaseEvents().isEmpty());
  }

  @Test
  public void shouldReturnModifiedCaseWhenAddressModified() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.HOUSEHOLD);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    mockDbHasCase();
    CaseDTO response = target.modifyCase(requestDTO);
    verifyCaseResponse(response, false);
    // verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
  }

  @Test
  public void shouldReturnModifiedCaseWhenAddressTypeChanged() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.CARE_HOME);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockDbHasCase();
    CaseDTO response = target.modifyCase(requestDTO);
    verifyCaseResponse(response, true);
    // verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
  }
}

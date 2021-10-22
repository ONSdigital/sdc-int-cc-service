package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.contactcentresvc.BlacklistedUPRNBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventTransfer;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public abstract class CaseServiceImplTestBase {
  @Spy AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Mock CaseDataClient caseDataClient;

  @Mock EqLaunchService eqLaunchService;

  @Mock EventTransfer eventTransfer;

  @Spy MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Mock BlacklistedUPRNBean blacklistedUPRNBean;

  static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  @InjectMocks CaseService target = new CaseServiceImpl();

  void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis >= minAllowed, actualInMillis + " not after " + minAllowed);
    assertTrue(actualInMillis <= maxAllowed, actualInMillis + " not before " + maxAllowed);
  }

  long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    return dateParser.parse(datetime).getTime();
  }

  <T extends EventPayload> T verifyEventSent(TopicType expectedEventType, Class<T> payloadClazz) {
    ArgumentCaptor<T> payloadCaptor = ArgumentCaptor.forClass(payloadClazz);
    verify(eventTransfer).send(eq(expectedEventType), payloadCaptor.capture());

    return payloadCaptor.getValue();
  }

  void verifyEventNotSent() {
    verify(eventTransfer, never()).send(any(), any());
  }

  void verifyCase(CaseDTO results, CaseDTO expectedCaseResult) throws Exception {
    assertEquals(expectedCaseResult.getId(), results.getId());
    assertEquals(expectedCaseResult.getCaseRef(), results.getCaseRef());
    assertEquals(
        expectedCaseResult.getAllowedDeliveryChannels(), results.getAllowedDeliveryChannels());
    assertEquals(expectedCaseResult, results);
    verifyEventNotSent();
  }

  CaseDTO createExpectedCaseDTO(Case caseFromDb) {
    CaseAddress addr = caseFromDb.getAddress();

    CaseAddressDTO addrDto =
        CaseAddressDTO.builder()
            .addressLine1(addr.getAddressLine1())
            .addressLine2(addr.getAddressLine2())
            .addressLine3(addr.getAddressLine3())
            .townName(addr.getTownName())
            .region(addr.getRegion())
            .postcode(addr.getPostcode())
            .uprn(createUprn(addr.getUprn()))
            .build();

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromDb.getId())
            .caseRef(caseFromDb.getCaseRef())
            .allowedDeliveryChannels(ALL_DELIVERY_CHANNELS)
            .address(addrDto)
            .caseEvents(Collections.emptyList())
            .build();
    return expectedCaseResult;
  }

  UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  Case mockGetCaseById(String region) throws Exception {
    Case caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    caze.getAddress().setRegion(Region.valueOf(region));
    mockGetCaseById(UUID_0, caze);
    return caze;
  }

  void mockGetCaseById(UUID id, Case result) throws Exception {
    when(caseDataClient.getCaseById(eq(id))).thenReturn(result);
  }

  void mockGetCaseById(UUID id, Exception ex) throws Exception {
    doThrow(ex).when(caseDataClient).getCaseById(eq(id));
  }

  void mockCaseEventWhiteList() {
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_UPDATE");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    lenient().when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }

  void assertCaseQIDRestClientFailureCaught(Exception ex, boolean caught) throws Exception {
    mockGetCaseById("W");
    Mockito.doThrow(ex)
        .when(caseServiceClient)
        .getSingleUseQuestionnaireId(eq(UUID_0), anyBoolean(), any());
    UACRequestDTO requestsFromCCSvc =
        UACRequestDTO.builder().adLocationId(AN_AGENT_ID).individual(true).build();
    try {
      target.getUACForCaseId(UUID_0, requestsFromCCSvc);
      fail();
    } catch (CTPException badRequest) {
      if (caught) {
        assertEquals(Fault.BAD_REQUEST, badRequest.getFault());
      } else {
        fail();
      }
    } catch (ResponseStatusException other) {
      if (!caught) {
        assertSame(ex, other);
      } else {
        fail();
      }
    }
  }
}

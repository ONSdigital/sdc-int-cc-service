package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientService;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.contactcentresvc.BlacklistedUPRNBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventTransfer;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public abstract class CaseServiceImplTestBase {
  @Spy AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientService caseServiceClient;

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
    assertEquals(expectedCaseResult, results);
    verifyEventNotSent();
  }

  CaseDTO createExpectedCaseDTO(Case caseFromDb) {

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromDb.getId())
            .caseRef(caseFromDb.getCaseRef())
            .sample(new HashMap<>(caseFromDb.getSample()))
            .sampleSensitive(new HashMap<>(caseFromDb.getSampleSensitive()))
            .interactions(Collections.emptyList())
            .build();
    return expectedCaseResult;
  }

  UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  Case mockGetCaseById(String region) throws Exception {
    Case caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
    caze.getSample().put("region", region);
    mockGetCaseById(UUID_0, caze);
    return caze;
  }

  void mockGetCaseById(UUID id, Case result) throws Exception {
    when(caseDataClient.getCaseById(eq(id))).thenReturn(result);
  }

  void mockGetCaseById(UUID id, Exception ex) throws Exception {
    doThrow(ex).when(caseDataClient).getCaseById(eq(id));
  }

  void mockRmGetCaseDTO(UUID id) {
    RmCaseDTO rmCaseDTO = FixtureHelper.loadPackageFixtures(RmCaseDTO[].class).get(0);
    
    lenient().when(caseServiceClient.getCaseById(id, true)).thenReturn(rmCaseDTO);
  }
  
  void mockCaseEventWhiteList() {
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_UPDATE");  // PMB change
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    lenient().when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }
}

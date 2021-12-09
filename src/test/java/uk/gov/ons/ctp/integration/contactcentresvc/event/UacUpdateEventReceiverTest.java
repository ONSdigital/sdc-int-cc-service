package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UacRepository;

@ExtendWith(MockitoExtension.class)
public class UacUpdateEventReceiverTest {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "b66e57b4-2a61-11ec-b90f-4c3275913db5";
  private static final String COLLECTION_EX_ID = "bdfc0ada-2a61-11ec-8c02-4c3275913db5";
  private static final String MESSAGE_ID = "3883af91-0052-4497-9805-3238544fcf8a";

  @Mock private CaseRepository caseRepo;
  @Mock private UacRepository uacRepository;
  @Mock private EventFilter eventFilter;

  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private UacUpdateEventReceiver target;

  @Captor private ArgumentCaptor<Uac> uacCaptor;
  @Captor private ArgumentCaptor<Case> caseCaptor;

  private UacEvent uacEvent;

  @BeforeEach
  public void setup() {
    uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
  }

  @Test
  public void shouldReceiveEvent() throws CTPException {
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(true);
    when(caseRepo.findById(UUID.fromString(CASE_ID))).thenReturn(Optional.of(new Case()));

    target.acceptEvent(uacEvent);

    verify(uacRepository).save(uacCaptor.capture());
    verify(caseRepo, times(0)).save(any());

    UacUpdate uacUpdate = uacEvent.getPayload().getUacUpdate();
    Uac uac = uacCaptor.getValue();
    verifyMappedUac(uac, uacUpdate);
  }

  @Test
  public void shouldRejectFilteredEvent() throws CTPException {
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(false);

    target.acceptEvent(uacEvent);

    verify(uacRepository, times(0)).save(any());
    verify(caseRepo, times(0)).findById(any());
    verify(caseRepo, times(0)).save(any());
  }

  @Test
  public void shouldReceiveEvent_NoExistingCase() throws CTPException {
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(true);
    target.acceptEvent(uacEvent);

    verify(uacRepository).save(uacCaptor.capture());
    verify(caseRepo).save(caseCaptor.capture());

    UacUpdate uacUpdate = uacEvent.getPayload().getUacUpdate();
    Uac uac = uacCaptor.getValue();
    verifyMappedUac(uac, uacUpdate);
    verifySkeletonCase(caseCaptor.getValue());
  }

  private void verifyMappedUac(Uac uac, UacUpdate uacUpdate) {
    assertEquals(uacUpdate.getCaseId(), uac.getCaseId().toString());
    assertEquals(uacUpdate.getSurveyId(), uac.getSurveyId().toString());
    assertEquals(uacUpdate.getCollectionExerciseId(), uac.getCollectionExerciseId().toString());
    assertEquals(uacUpdate.getUacHash(), uac.getUacHash());
    assertEquals(uacUpdate.getMetadata().getWave(), uac.getWaveNum());
    assertEquals(uacUpdate.getCollectionInstrumentUrl(), uac.getCollectionInstrumentUrl());
    assertEquals(uacUpdate.getQid(), uac.getQuestionnaire());
    assertEquals(uacUpdate.isActive(), uac.isActive());
    assertEquals(uacUpdate.isReceiptReceived(), uac.isReceiptReceived());
    assertEquals(uacUpdate.isEqLaunched(), uac.isEqLaunched());
  }

  private void verifySkeletonCase(Case collectionCase) {
    assertEquals(CASE_ID, collectionCase.getId().toString());
    assertEquals(CCStatus.PENDING, collectionCase.getCcStatus());
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import javax.persistence.PersistenceException;
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
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseUpdateEventReceiverTest {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "b66e57b4-2a61-11ec-b90f-4c3275913db5";
  private static final String COLLECTION_EX_ID = "bdfc0ada-2a61-11ec-8c02-4c3275913db5";
  private static final String MESSAGE_ID = "3883af91-0052-4497-9805-3238544fcf8a";

  @Mock private CaseRepository caseRepo;
  @Mock private EventFilter eventFilter;

  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private CaseUpdateEventReceiver target;

  @Captor private ArgumentCaptor<Case> caseCaptor;

  private CaseEvent caseEvent;

  @BeforeEach
  public void setup() {
    caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  }

  @Test
  public void shouldReceiveEvent() throws CTPException {
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(true);
    target.acceptEvent(caseEvent);

    verify(caseRepo).saveAndFlush(caseCaptor.capture());

    CaseUpdate ccase = caseEvent.getPayload().getCaseUpdate();
    Case caze = caseCaptor.getValue();
    verifyMappedCase(caze, ccase);
  }

  @Test
  public void shouldRejectFilteredEvent() throws CTPException {
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(false);
    target.acceptEvent(caseEvent);

    verify(caseRepo, times(0)).saveAndFlush(caseCaptor.capture());
  }

  @Test
  public void shouldRejectFailingSave() throws CTPException {
    when(caseRepo.saveAndFlush(any())).thenThrow(PersistenceException.class);
    when(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID))
        .thenReturn(true);
    assertThrows(PersistenceException.class, () -> target.acceptEvent(caseEvent));
  }

  private void verifyMappedCase(Case caze, CaseUpdate ccase) {
    var sample = ccase.getSample();
    assertEquals(UUID.fromString(CASE_ID), caze.getId());
    assertEquals(UUID.fromString(COLLECTION_EX_ID), caze.getCollectionExercise().getId());
    assertTrue(caze.isInvalid());
    assertEquals(RefusalType.HARD_REFUSAL, caze.getRefusalReceived());
    assertEquals(sample.getQuestionnaire(), caze.getQuestionnaire());
    assertEquals(sample.getSampleUnitRef(), caze.getSampleUnitRef());
    assertEquals(sample.getCohort(), caze.getCohort());

    CaseContact contact = caze.getContact();
    CaseAddress address = caze.getAddress();
    assertEquals(expectedContact(ccase.getSampleSensitive()), contact);
    assertEquals(expectedAddress(sample), address);
    assertEquals(ccase.getCaseRef(), caze.getCaseRef());
    assertEquals(toLocalDateTime(ccase.getCreatedAt()), caze.getCreatedAt());
    assertEquals(toLocalDateTime(ccase.getLastUpdatedAt()), caze.getLastUpdatedAt());
  }

  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
  }

  private CaseContact expectedContact(CaseUpdateSampleSensitive sensitive) {
    return CaseContact.builder().phoneNumber(sensitive.getPhoneNumber()).build();
  }

  private CaseAddress expectedAddress(CaseUpdateSample addr) {
    return CaseAddress.builder()
        .uprn(addr.getUprn())
        .addressLine1(addr.getAddressLine1())
        .addressLine2(addr.getAddressLine2())
        .addressLine3(addr.getAddressLine3())
        .townName(addr.getTownName())
        .postcode(addr.getPostcode())
        .region(Region.valueOf(addr.getRegion()))
        .gor9d(addr.getGor9d())
        .laCode(addr.getLaCode())
        .uprnLatitude(addr.getUprnLatitude())
        .uprnLongitude(addr.getUprnLongitude())
        .build();
  }
}

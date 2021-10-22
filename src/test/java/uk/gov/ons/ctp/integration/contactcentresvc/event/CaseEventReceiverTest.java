package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverTest {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "569a7020-324f-11ec-9a07-4c3275913db5";
  private static final String COLLECTION_EX_ID = "bdfc0ada-2a61-11ec-8c02-4c3275913db5";

  @Mock private CaseRepository caseRepo;
  @Mock private SurveyRepository surveyRepo;
  @Mock private CollectionExerciseRepository collExRepo;

  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private CaseEventReceiver target;

  @Captor private ArgumentCaptor<Case> caseCaptor;

  private CaseEvent caseEvent;

  @BeforeEach
  public void setup() {
    caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  }

  @Test
  public void shouldReceiveEvent() {
    mockSocialSurvey();
    mockCollectionExercise();
    target.acceptEvent(caseEvent);

    verify(caseRepo).save(caseCaptor.capture());

    CaseUpdate ccase = caseEvent.getPayload().getCaseUpdate();
    Case caze = caseCaptor.getValue();
    verifyMappedCase(caze, ccase);
  }

  @Test
  public void shouldDiscardEventWithUnknownSurvey() {
    when(surveyRepo.getById(any())).thenReturn(null);
    target.acceptEvent(caseEvent);
    verify(collExRepo, never()).getById(any());
    verify(caseRepo, never()).save(any());
  }

  @Test
  public void shouldDiscardEventWithNonSocialSurvey() {
    mockSurvey("test/somethingelse.json");
    target.acceptEvent(caseEvent);
    verify(collExRepo, never()).getById(any());
    verify(caseRepo, never()).save(any());
  }

  @Test
  public void shouldDiscardEventWithUnknownCollectionExercise() {
    mockSocialSurvey();
    when(collExRepo.getById(any())).thenReturn(null);
    target.acceptEvent(caseEvent);
    verify(caseRepo, never()).save(any());
  }

  @Test
  public void shouldRejectFailingSave() {
    mockSocialSurvey();
    mockCollectionExercise();
    when(caseRepo.save(any())).thenThrow(PersistenceException.class);
    assertThrows(PersistenceException.class, () -> target.acceptEvent(caseEvent));
  }

  private void mockSocialSurvey() {
    mockSurvey("test/social.json");
  }

  private void mockSurvey(String url) {
    Survey survey = new Survey();
    survey.setId(UUID.fromString(SURVEY_ID));
    survey.setSampleDefinitionUrl(url);
    when(surveyRepo.getById(any())).thenReturn(survey);
  }

  private void mockCollectionExercise() {
    CollectionExercise collEx = new CollectionExercise();
    collEx.setId(UUID.fromString(COLLECTION_EX_ID));
    when(collExRepo.getById(any())).thenReturn(collEx);
  }

  private void verifyMappedCase(Case caze, CaseUpdate ccase) {
    assertEquals(UUID.fromString(CASE_ID), caze.getId());
    assertEquals(UUID.fromString(COLLECTION_EX_ID), caze.getCollectionExercise().getId());
    assertTrue(caze.isInvalid());
    assertEquals(RefusalType.HARD_REFUSAL, caze.getRefusalReceived());

    CaseContact contact = caze.getContact();
    CaseAddress address = caze.getAddress();
    assertEquals(expectedContact(ccase.getSampleSensitive()), contact);
    assertEquals(expectedAddress(ccase.getSample()), address);
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
        .build();
  }
}

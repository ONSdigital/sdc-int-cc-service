package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@ExtendWith(MockitoExtension.class)
public class EventFilerTest {
  private static final Set<String> ACCEPTED_SURVEYS = Set.of("social", "asteroid");
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "b66e57b4-2a61-11ec-b90f-4c3275913db5";
  private static final String COLLECTION_EX_ID = "bdfc0ada-2a61-11ec-8c02-4c3275913db5";
  private static final String MESSAGE_ID = "3883af91-0052-4497-9805-3238544fcf8a";

  @Mock private SurveyRepository surveyRepo;
  @Mock private CollectionExerciseRepository collExRepo;
  @Mock AppConfig appConfig;

  @InjectMocks private EventFilter eventFilter;

  @Test
  public void shouldAcceptEventWithAllPrerequisiteEvents() {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    mockSocialSurvey();
    mockCollectionExercise();
    assertTrue(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID));
  }

  @Test
  public void shouldDiscardEventWithUnknownSurvey() {
    when(surveyRepo.findById(any())).thenReturn(Optional.empty());
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID));
    verify(collExRepo, never()).getById(any());
  }

  @Test
  public void shouldDiscardEventWithNonSocialSurvey() {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    mockSurvey("test/somethingelse.json");
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID));
    verify(collExRepo, never()).getById(any());
  }

  @Test
  public void shouldDiscardEventWithUnknownCollectionExercise() {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    mockSocialSurvey();
    when(collExRepo.findById(any())).thenReturn(Optional.empty());
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLECTION_EX_ID, CASE_ID, MESSAGE_ID));
  }

  private void mockSocialSurvey() {
    mockSurvey("test/social.json");
  }

  private void mockSurvey(String url) {
    Survey survey = new Survey();
    survey.setId(UUID.fromString(SURVEY_ID));
    survey.setSampleDefinitionUrl(url);
    when(surveyRepo.findById(any())).thenReturn(Optional.of(survey));
  }

  private void mockCollectionExercise() {
    CollectionExercise collEx = new CollectionExercise();
    collEx.setId(UUID.fromString(COLLECTION_EX_ID));
    when(collExRepo.findById(any())).thenReturn(Optional.of(collEx));
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PostgresTestBase;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

public class CaseEventReceiverIT extends PostgresTestBase {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";
  private static final String COLLECTION_EX_ID = "4883af91-0052-4497-9805-3238544fcf8a";

  @Autowired private CaseRepository caseRepo;
  @Autowired private TransactionalOps txOps;

  private CaseEvent caseEvent;
  private CaseUpdate caseUpdate;
  private Survey survey;

  @BeforeEach
  public void setup() {
    txOps.deleteItems();
    caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseUpdate = caseEvent.getPayload().getCaseUpdate();
    caseUpdate.setCaseId(CASE_ID);
    caseUpdate.setCollectionExerciseId(COLLECTION_EX_ID);
    caseUpdate.setSurveyId(SURVEY_ID);
  }

  @Test
  public void shouldReceiveCase() {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    Case caze = caseRepo.getById(UUID.fromString(CASE_ID));
    assertNotNull(caze);
    assertEquals("10000000017", caze.getCaseRef());
    assertEquals("AB1 2ZX", caze.getAddress().getPostcode());
    assertEquals("CC3", caze.getCohort());
  }

  @Test
  public void shouldRejectCaseForIgnoredSurveyType() {
    survey = txOps.createSurveyWeFilterOut(UUID.fromString(SURVEY_ID));
    txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectCaseForMissingSurvey() {
    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectCaseForMissingCollectionExercise() {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private CaseRepository caseRepo;
    private SurveyRepository surveyRepo;
    private CollectionExerciseRepository collExRepo;
    private CaseEventReceiver target;

    public TransactionalOps(
        SurveyRepository surveyRepo,
        CollectionExerciseRepository collExRepo,
        CaseRepository caseRepo,
        CaseEventReceiver target) {
      this.surveyRepo = surveyRepo;
      this.collExRepo = collExRepo;
      this.caseRepo = caseRepo;
      this.target = target;
    }

    public void deleteItems() {
      deleteIfExists(caseRepo, UUID.fromString(CASE_ID));
      deleteIfExists(collExRepo, UUID.fromString(COLLECTION_EX_ID));
      deleteIfExists(surveyRepo, UUID.fromString(SURVEY_ID));
    }

    private void deleteIfExists(JpaRepository<?, UUID> repo, UUID id) {
      repo.findById(id)
          .ifPresent(
              item -> {
                repo.deleteById(id);
              });
    }

    public Survey createSurvey(UUID id) {
      Survey survey =
          Survey.builder()
              .id(id)
              .name("LMS")
              .sampleDefinitionUrl("https://some.domain/social.json")
              .sampleDefinition("{}")
              .build();
      surveyRepo.save(survey);
      return survey;
    }

    public Survey createSurveyWeFilterOut(UUID id) {
      Survey survey =
          Survey.builder()
              .id(id)
              .name("LMS")
              .sampleDefinitionUrl("https://some.domain/test.json")
              .sampleDefinition("{}")
              .build();
      surveyRepo.save(survey);
      return survey;
    }

    public void createCollex(Survey survey, UUID id) {
      CollectionExercise cx =
          CollectionExercise.builder()
              .id(id)
              .survey(survey)
              .name("gregory")
              .reference("MVP012021")
              .startDate(LocalDateTime.now())
              .endDate(LocalDateTime.now().plusDays(1))
              .build();
      collExRepo.save(cx);
    }

    public void acceptEvent(CaseEvent event) {
      target.acceptEvent(event);
    }
  }
}

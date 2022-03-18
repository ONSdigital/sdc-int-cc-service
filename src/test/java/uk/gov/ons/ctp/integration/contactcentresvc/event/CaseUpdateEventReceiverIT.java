package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PostgresTestBase;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;

public class CaseUpdateEventReceiverIT extends PostgresTestBase {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";
  private static final String COLLECTION_EX_ID = "4883af91-0052-4497-9805-3238544fcf8a";

  @Autowired private CaseRepository caseRepo;
  @Autowired private TransactionalOps txOps;

  private CaseEvent caseEvent;
  private CaseUpdate caseUpdate;
  private Survey survey;
  private CollectionExercise collectionExercise;

  @BeforeEach
  public void setup() {
    txOps.deleteAll();
    caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseUpdate = caseEvent.getPayload().getCaseUpdate();
    caseUpdate.setCaseId(CASE_ID);
    caseUpdate.setCollectionExerciseId(COLLECTION_EX_ID);
    caseUpdate.setSurveyId(SURVEY_ID);
  }

  @Test
  public void shouldReceiveCase() throws CTPException {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    Case caze = caseRepo.getById(UUID.fromString(CASE_ID));
    assertNotNull(caze);
    assertEquals("10000000017", caze.getCaseRef());
    assertEquals("AB1 2ZX", caze.getSample().get(CaseUpdate.ATTRIBUTE_POSTCODE));
    assertEquals("CC3", caze.getSample().get(CaseUpdate.ATTRIBUTE_COHORT));
  }

  @Test
  public void shouldRejectCaseForIgnoredSurveyType() throws CTPException {
    survey = txOps.createSurveyWeFilterOut(UUID.fromString(SURVEY_ID));
    txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    txOps.acceptEvent(caseEvent);

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectCaseForMissingSurvey() {
    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(caseEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectCaseForMissingCollectionExercise() {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));

    assertFalse(caseRepo.existsById(UUID.fromString(CASE_ID)));

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(caseEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(caseRepo.findById(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldPopulateSkeletonCase() throws CTPException {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    collectionExercise = txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));
    txOps.createSkeletonCase(collectionExercise, UUID.fromString(CASE_ID));

    Case skeletonCase = caseRepo.getById(UUID.fromString(CASE_ID));
    assertNotNull(skeletonCase);

    txOps.acceptEvent(caseEvent);

    Case caze = caseRepo.getById(UUID.fromString(CASE_ID));
    assertNotNull(caze);
    assertEquals("10000000017", caze.getCaseRef());
    assertEquals("AB1 2ZX", caze.getSample().get(CaseUpdate.ATTRIBUTE_POSTCODE));
    assertEquals("CC3", caze.getSample().get(CaseUpdate.ATTRIBUTE_COHORT));
    assertEquals(CCStatus.READY, caze.getCcStatus());
    assertNotNull(caze.getSampleSensitive());
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PostgresTestBase;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UacRepository;

public class UacUpdateEventReceiverIT extends PostgresTestBase {

  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";
  private static final String COLLECTION_EX_ID = "4883af91-0052-4497-9805-3238544fcf8a";

  @Autowired private CaseRepository caseRepo;
  @Autowired private UacRepository uacRepository;
  @Autowired private TransactionalOps txOps;

  private UacEvent uacEvent;
  private UacUpdate uacUpdate;
  private Survey survey;
  private CollectionExercise collectionExercise;

  @BeforeEach
  public void setup() {
    txOps.deleteAll();
    uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
    uacUpdate = uacEvent.getPayload().getUacUpdate();
    uacUpdate.setCaseId(CASE_ID);
    uacUpdate.setCollectionExerciseId(COLLECTION_EX_ID);
    uacUpdate.setSurveyId(SURVEY_ID);
  }

  @Test
  public void shouldReceiveUac() throws CTPException {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    collectionExercise = txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));
    txOps.createCase(collectionExercise, UUID.fromString(CASE_ID));

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());

    txOps.acceptEvent(uacEvent);

    List<Uac> uacList = uacRepository.findByCaseId(UUID.fromString(CASE_ID));
    boolean matched = false;
    for (Uac uac : uacList) {
      if (uac.getUacHash().equals(uacEvent.getPayload().getUacUpdate().getUacHash())) {
        assertNotNull(uac);
        assertEquals(CASE_ID, uac.getCaseId().toString());
        assertEquals(uacUpdate.getUacHash(), uac.getUacHash());
        assertEquals(COLLECTION_EX_ID, uac.getCollectionExerciseId().toString());
        assertEquals(uacUpdate.getQid(), uac.getQuestionnaire());
        assertEquals(SURVEY_ID, uac.getSurveyId().toString());
        assertEquals(uacUpdate.getMetadata().getWave(), uac.getWaveNum());
        assertEquals(uacUpdate.getCollectionInstrumentUrl(), uac.getCollectionInstrumentUrl());
        assertNotNull(uac.getId());
        matched = true;
        break;
      }
    }
    assertTrue(matched, "Did not find the stored UAC");
  }

  @Test
  public void shouldDiscardUacForIgnoredSurveyType() throws CTPException {
    survey = txOps.createSurveyWeFilterOut(UUID.fromString(SURVEY_ID));

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());

    txOps.acceptEvent(uacEvent);

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectUacMessageForMissingSurvey() {
    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(uacEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectUacMessageForMissingCollectionExercise() {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(uacEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldSaveUacAndSkeletonCaseWhenCaseNotFound() throws CTPException {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    collectionExercise = txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
    assertFalse(caseRepo.findById(UUID.fromString(CASE_ID)).isPresent());

    txOps.acceptEvent(uacEvent);

    List<Uac> uacList = uacRepository.findByCaseId(UUID.fromString(CASE_ID));
    boolean matched = false;
    for (Uac uac : uacList) {
      if (uac.getUacHash().equals(uacEvent.getPayload().getUacUpdate().getUacHash())) {
        assertNotNull(uac);
        assertEquals(CASE_ID, uac.getCaseId().toString());
        assertEquals(uacUpdate.getUacHash(), uac.getUacHash());
        assertEquals(COLLECTION_EX_ID, uac.getCollectionExerciseId().toString());
        assertEquals(uacUpdate.getQid(), uac.getQuestionnaire());
        assertEquals(SURVEY_ID, uac.getSurveyId().toString());
        assertEquals(uacUpdate.getMetadata().getWave(), uac.getWaveNum());
        assertEquals(uacUpdate.getCollectionInstrumentUrl(), uac.getCollectionInstrumentUrl());
        assertNotNull(uac.getId());
        matched = true;
        break;
      }
    }
    assertTrue(matched, "Did not find the stored UAC");

    Case caze = caseRepo.findById(UUID.fromString(CASE_ID)).orElse(null);
    assertNotNull(caze);
    assertEquals(CCStatus.PENDING, caze.getCcStatus());
    assertEquals("", caze.getCaseRef());
    assertEquals(LocalDateTime.parse("9999-01-01T00:00:00.000"), caze.getLastUpdatedAt());
    assertEquals(LocalDateTime.parse("9999-01-01T00:00:00.000"), caze.getCreatedAt());
    assertEquals(COLLECTION_EX_ID, caze.getCollectionExercise().getId().toString());
    assertEquals(Map.of("", ""), caze.getSample());
    assertEquals(Map.of("", ""), caze.getSampleSensitive());
    assertFalse(caze.isInvalid());
    assertNull(caze.getRefusalReceived());
  }
}

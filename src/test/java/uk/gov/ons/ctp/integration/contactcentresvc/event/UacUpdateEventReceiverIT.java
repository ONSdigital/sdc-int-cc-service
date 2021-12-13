package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PostgresTestBase;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
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
    txOps.deleteItems();
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

    assertFalse(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isPresent());

    txOps.acceptEvent(uacEvent);

    Uac uac = uacRepository.findByCaseId(UUID.fromString(CASE_ID)).orElse(null);
    assertNotNull(uac);
    assertEquals(CASE_ID, uac.getCaseId().toString());
    assertEquals(uacUpdate.getUacHash(), uac.getUacHash());
    assertEquals(COLLECTION_EX_ID, uac.getCollectionExerciseId().toString());
    assertEquals(uacUpdate.getQid(), uac.getQuestionnaire());
    assertEquals(SURVEY_ID, uac.getSurveyId().toString());
    assertEquals(uacUpdate.getMetadata().getWave(), uac.getWaveNum());
    assertEquals(uacUpdate.getCollectionInstrumentUrl(), uac.getCollectionInstrumentUrl());
    assertNotNull(uac.getId());
  }

  @Test
  public void shouldDiscardUacForIgnoredSurveyType() throws CTPException {
    survey = txOps.createSurveyWeFilterOut(UUID.fromString(SURVEY_ID));

    assertFalse(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isPresent());

    txOps.acceptEvent(uacEvent);

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectUacMessageForMissingSurvey() {
    assertFalse(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isPresent());

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(uacEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldRejectUacMessageForMissingCollectionExercise() {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));

    assertFalse(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isPresent());

    CTPException thrown = assertThrows(CTPException.class, () -> txOps.acceptEvent(uacEvent));
    assertEquals(CTPException.Fault.VALIDATION_FAILED, thrown.getFault());

    assertTrue(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isEmpty());
  }

  @Test
  public void shouldSaveUacAndSkeletonCaseWhenCaseNotFound() throws CTPException {
    survey = txOps.createSurvey(UUID.fromString(SURVEY_ID));
    collectionExercise = txOps.createCollex(survey, UUID.fromString(COLLECTION_EX_ID));

    assertFalse(uacRepository.findByCaseId(UUID.fromString(CASE_ID)).isPresent());
    assertFalse(caseRepo.findById(UUID.fromString(CASE_ID)).isPresent());

    txOps.acceptEvent(uacEvent);

    Uac uac = uacRepository.findByCaseId(UUID.fromString(CASE_ID)).orElse(null);
    assertNotNull(uac);
    assertEquals(CASE_ID, uac.getCaseId().toString());
    assertEquals(uacUpdate.getUacHash(), uac.getUacHash());
    assertEquals(COLLECTION_EX_ID, uac.getCollectionExerciseId().toString());
    assertEquals(uacUpdate.getQid(), uac.getQuestionnaire());
    assertEquals(SURVEY_ID, uac.getSurveyId().toString());
    assertEquals(uacUpdate.getMetadata().getWave(), uac.getWaveNum());
    assertEquals(uacUpdate.getCollectionInstrumentUrl(), uac.getCollectionInstrumentUrl());
    assertNotNull(uac.getId());

    CaseAddress address =
        CaseAddress.builder()
            .uprn("")
            .addressLine1("")
            .townName("")
            .postcode("")
            .region(Region.E)
            .build();

    Case caze = caseRepo.findById(UUID.fromString(CASE_ID)).orElse(null);
    assertNotNull(caze);
    assertEquals(CCStatus.PENDING, caze.getCcStatus());
    assertEquals("", caze.getCaseRef());
    assertEquals(LocalDateTime.parse("9999-01-01T00:00:00.000"), caze.getLastUpdatedAt());
    assertEquals(LocalDateTime.parse("9999-01-01T00:00:00.000"), caze.getCreatedAt());
    assertEquals(COLLECTION_EX_ID, caze.getCollectionExercise().getId().toString());
    assertEquals(address, caze.getAddress());
    assertFalse(caze.isInvalid());
    assertNull(caze.getContact());
    assertNull(caze.getCohort());
    assertNull(caze.getSampleUnitRef());
    assertNull(caze.getRefusalReceived());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UacRepository uacRepository;
    private CaseRepository caseRepository;
    private SurveyRepository surveyRepo;
    private CollectionExerciseRepository collExRepo;
    private UacUpdateEventReceiver target;

    public TransactionalOps(
        SurveyRepository surveyRepo,
        CollectionExerciseRepository collExRepo,
        UacRepository uacRepository,
        CaseRepository caseRepository,
        UacUpdateEventReceiver target) {
      this.surveyRepo = surveyRepo;
      this.collExRepo = collExRepo;
      this.uacRepository = uacRepository;
      this.caseRepository = caseRepository;
      this.target = target;
    }

    public void deleteItems() {
      deleteUacIfExists(uacRepository, UUID.fromString(CASE_ID));
      deleteIfExists(caseRepository, UUID.fromString(CASE_ID));
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

    private void deleteUacIfExists(UacRepository repo, UUID id) {
      repo.findByCaseId(id)
          .ifPresent(
              item -> {
                repo.deleteById(item.getId());
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
      surveyRepo.saveAndFlush(survey);
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
      surveyRepo.saveAndFlush(survey);
      return survey;
    }

    public CollectionExercise createCollex(Survey survey, UUID id) {
      CollectionExercise cx =
          CollectionExercise.builder()
              .id(id)
              .survey(survey)
              .name("gregory")
              .reference("MVP012021")
              .startDate(LocalDateTime.now())
              .endDate(LocalDateTime.now().plusDays(1))
              .build();
      collExRepo.saveAndFlush(cx);
      return cx;
    }

    public void createCase(CollectionExercise collectionExercise, UUID id) {
      Case collectionCase =
          Case.builder()
              .id(id)
              .collectionExercise(collectionExercise)
              .caseRef("1")
              .address(
                  CaseAddress.builder()
                      .uprn("1234")
                      .addressLine1("1 Street Name")
                      .townName("Town")
                      .postcode("PO57 6DE")
                      .region(Region.E)
                      .build())
              .ccStatus(CCStatus.READY)
              .cohort("1")
              .contact(new CaseContact())
              .createdAt(LocalDateTime.parse("2021-12-01T00:00:00.000"))
              .invalid(false)
              .lastUpdatedAt(LocalDateTime.parse("2021-12-01T00:00:00.000"))
              .questionnaire("1")
              .refusalReceived(RefusalType.EXTRAORDINARY_REFUSAL)
              .sampleUnitRef("unit")
              .build();
      caseRepository.saveAndFlush(collectionCase);
    }

    public void acceptEvent(UacEvent event) throws CTPException {
      target.acceptEvent(event);
    }
  }
}

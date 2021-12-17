package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

public class CaseRepositoryIT extends PostgresTestBase {

  @Autowired private CaseTransactionalOps txOps;

  private Case inputCase;

  @BeforeEach
  public void setup() throws Exception {
    txOps.deleteAll();
    // Survey and collex required to exist in order to save case
    Survey survey = FixtureHelper.loadClassFixtures(Survey[].class).get(0);
    CollectionExercise collex = FixtureHelper.loadClassFixtures(CollectionExercise[].class).get(0);
    inputCase = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    txOps.writeSurvey(survey);
    txOps.writeCollex(collex);
    txOps.writeCase(inputCase);
  }

  @Test
  public void searchSampleAttributes_ExactValue() throws Exception {
    Case searchResult = txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_REGION, "W").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_REGION, "W");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_PartialValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1, "1 Test").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1, "1 Test Road");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_AllUpperCaseValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_2, "TESTPARK").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_ADDRESS_LINE_2, "Testpark");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_AllLowerCaseValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_3, "test").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_ADDRESS_LINE_3, "Test");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_MixedCaseValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_TOWN_NAME, "faKE tOwN").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_TOWN_NAME, "Fake Town");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_CorrectSpaceInValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_POSTCODE, "TE57 6DE").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_POSTCODE, "TE57 6DE");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_ExtraSpaceInValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_POSTCODE, "TE57  6DE").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_POSTCODE, "TE57 6DE");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_MissingSpaceInValue() throws Exception {
    Case searchResult =
        txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_POSTCODE, "TE576DE").get(0);

    verifyResult(searchResult, CaseUpdate.ATTRIBUTE_POSTCODE, "TE57 6DE");
    assertEquals(inputCase.getSample(), searchResult.getSample());
  }

  @Test
  public void searchSampleAttributes_NoMatch() throws Exception {
    assertTrue(txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_POSTCODE, "WR0NGPC").isEmpty());
  }

  @Test
  public void searchSampleAttributes_MultipleMatches() throws Exception {
    Case secondaryCase = FixtureHelper.loadClassFixtures(Case[].class).get(1);
    txOps.writeCase(secondaryCase);
    assertEquals(2, txOps.searchSampleAttributes(CaseUpdate.ATTRIBUTE_POSTCODE, "TE57 6DE").size());
  }

  private void verifyResult(Case searchResult, String key, String actualExpectedValue) {
    assertEquals(inputCase.getId(), searchResult.getId());
    assertEquals(inputCase.getCaseRef(), searchResult.getCaseRef());
    assertEquals(inputCase.getCcStatus(), searchResult.getCcStatus());
    assertEquals(inputCase.getCreatedAt(), searchResult.getCreatedAt());
    assertEquals(
        inputCase.getCollectionExercise().getId(), searchResult.getCollectionExercise().getId());
    assertEquals(inputCase.getRefusalReceived(), searchResult.getRefusalReceived());
    assertEquals(inputCase.getLastUpdatedAt(), searchResult.getLastUpdatedAt());
    assertEquals(inputCase.getCreatedAt(), searchResult.getCreatedAt());
    assertEquals(inputCase.getSampleSensitive(), searchResult.getSampleSensitive());

    assertEquals(inputCase.getSample(), searchResult.getSample());
    assertTrue(searchResult.getSample().size() > 0);
    assertThat(searchResult.getSample().get(key)).contains(actualExpectedValue);
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class CaseTransactionalOps {
    private CaseRepository caseRepo;
    private CollectionExerciseRepository collectionExerciseRepository;
    private SurveyRepository surveyRepository;
    private CCSvcBeanMapper ccBeanMapper;

    public CaseTransactionalOps(
        CaseRepository caseRepo,
        CCSvcBeanMapper ccBeanMapper,
        CollectionExerciseRepository collectionExerciseRepository,
        SurveyRepository surveyRepository) {
      this.caseRepo = caseRepo;
      this.ccBeanMapper = ccBeanMapper;
      this.collectionExerciseRepository = collectionExerciseRepository;
      this.surveyRepository = surveyRepository;
    }

    public void deleteAll() {
      caseRepo.deleteAll();
      collectionExerciseRepository.deleteAll();
      surveyRepository.deleteAll();
    }

    public void writeCollex(CollectionExercise testCollex) {
      collectionExerciseRepository.saveAndFlush(testCollex);
    }

    public void writeSurvey(Survey testSurvey) {
      surveyRepository.saveAndFlush(testSurvey);
    }

    public void writeCase(Case testCase) {
      caseRepo.saveAndFlush(testCase);
    }

    public List<Case> searchSampleAttributes(String key, String value) {

      return caseRepo.findBySampleContains(key, value);
    }
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonRawValue;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.event.SurveyEventReceiver;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

public class SurveyRepositoryIT extends PostgresTestBase {

  @Autowired private SurveyRepository repo;
  @Autowired private CollectionExerciseRepository collExRepo;
  @Autowired private SurveyTransactionalOps txOps;
  @Autowired private CCSvcBeanMapper ccBeanMapper;
  
  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }
  
  @Test
  public void shouldLoadNewSurvey() throws Exception {
    
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class).get(0);
    
   // SurveyEventReceiver surveyReceiver = new SurveyEventReceiver(repo, ccBeanMapper);
   // surveyReceiver.acceptEvent(surveyUpdateEvent);
    
    txOps.loadSurvey(surveyUpdateEvent);
    
    //Survey loadedSurveyPMB = txOps.readSurveyById(UUID.randomUUID().toString());
    
    String surveyId = surveyUpdateEvent.getPayload().getSurveyUpdate().getSurveyId();
    Survey loadedSurvey = repo.getById(UUID.fromString(surveyId));
    
    verifySurveysAreEqual(surveyUpdateEvent.getPayload().getSurveyUpdate(), loadedSurvey);
    
    assertEquals(surveyUpdateEvent, loadedSurvey);
    
  }

  private void verifySurveysAreEqual(SurveyUpdate testSurvey, Survey loadedSurvey) {
    assertEquals(testSurvey.getSurveyId(), loadedSurvey.getId().toString());
    assertEquals(testSurvey.getName(), loadedSurvey.getName());
    assertEquals(testSurvey.getSampleDefinitionUrl(), loadedSurvey.getSampleDefinitionUrl());
    //PMB assertEquals(testSurvey.getSampleDefinition(), loadedSurvey.getSampleDefinition());
    
    //PMB check metadata
    
    verifyProductsAreEqual(testSurvey.getAllowedPrintFulfilments(), loadedSurvey.getAllowedPrintFulfilments());
    verifyProductsAreEqual(testSurvey.getAllowedPrintFulfilments(), loadedSurvey.getAllowedSmsFulfilments());
    verifyProductsAreEqual(testSurvey.getAllowedPrintFulfilments(), loadedSurvey.getAllowedEmailFulfilments());
  }

  private void verifyProductsAreEqual(List<SurveyFulfilment> expectedFulfilments,
      List<Product> actualProducts) {
    assertEquals(actualProducts.toString(), expectedFulfilments.size(), actualProducts.size());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class SurveyTransactionalOps {
    private SurveyRepository repo;
    private CollectionExerciseRepository collExRepo;
    private CCSvcBeanMapper ccBeanMapper;
    
    public SurveyTransactionalOps(SurveyRepository repo, CollectionExerciseRepository collExRepo, CCSvcBeanMapper ccBeanMapper) {
      this.repo = repo;
      this.collExRepo = collExRepo;
      this.ccBeanMapper = ccBeanMapper;
    }

    public void deleteAll() {
      collExRepo.deleteAll();
      repo.deleteAll();
    }
    
    public void loadSurvey(SurveyUpdateEvent surveyUpdateEvent) throws Exception {
      SurveyEventReceiver surveyReceiver = new SurveyEventReceiver(repo, ccBeanMapper);
      surveyReceiver.acceptEvent(surveyUpdateEvent);
    }

    public Survey readSurveyById(String surveyId) {
      UUID surveyUUID = UUID.fromString(surveyId);
      return repo.getById(surveyUUID);
    }
  }
}

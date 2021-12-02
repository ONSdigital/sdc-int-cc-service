package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.event.SurveyEventReceiver;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

public class SurveyRepositoryIT extends PostgresTestBase {

  @Autowired private SurveyRepository repo;
  @Autowired private SurveyTransactionalOps txOps;
  
  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }
  
  @Test
  public void shouldLoadNewSurvey() throws Exception {
    
    // Load in the survey under test
    List<SurveyUpdateEvent> testSurveys = FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class);
    SurveyUpdateEvent surveyUpdateEvent = testSurveys.get(0);
    txOps.loadSurvey(surveyUpdateEvent);
    
    // Load another survey to make sure its data doesn't interact with the test survey
    List<SurveyUpdateEvent> secondarySurveys = FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class, "Secondary");
    SurveyUpdateEvent secondarySurveyUpdateEvent = secondarySurveys.get(0);
    txOps.loadSurvey(secondarySurveyUpdateEvent);
    
    // Confirm survey+products stored correctly in db
    SurveyUpdate testSurvey = surveyUpdateEvent.getPayload().getSurveyUpdate();
    String surveyId = testSurvey.getSurveyId();
    Survey loadedSurvey = repo.getById(UUID.fromString(surveyId));
    verifySurveysAreEqual(testSurvey, loadedSurvey);
  }

  private void verifySurveysAreEqual(SurveyUpdate testSurvey, Survey loadedSurvey) {
    assertEquals(testSurvey.getSurveyId(), loadedSurvey.getId().toString());
    assertEquals(testSurvey.getName(), loadedSurvey.getName());
    assertEquals(testSurvey.getSampleDefinitionUrl(), loadedSurvey.getSampleDefinitionUrl());
    //PMB assertEquals(testSurvey.getSampleDefinition(), loadedSurvey.getSampleDefinition());
    //PMB check metadata
    
    // Verify that the number of loaded fulfilments is correct
    int totalNumFulfilments = testSurvey.getAllowedPrintFulfilments().size() 
        + testSurvey.getAllowedSmsFulfilments().size() 
        + testSurvey.getAllowedEmailFulfilments().size();
    List<Product> allowedFulfilments = loadedSurvey.getAllowedFulfilments();
    assertEquals(totalNumFulfilments, allowedFulfilments.size());
    
    // Confirm products loaded
    verifyProductsLoaded(ProductType.POSTAL, testSurvey.getAllowedPrintFulfilments(), allowedFulfilments);
    verifyProductsLoaded(ProductType.SMS, testSurvey.getAllowedSmsFulfilments(), allowedFulfilments);
    verifyProductsLoaded(ProductType.EMAIL, testSurvey.getAllowedEmailFulfilments(), allowedFulfilments);
  }

  private void verifyProductsLoaded(ProductType productType, List<SurveyFulfilment> expectedFulfilments,
      List<Product> actualProducts) {
    
    for (SurveyFulfilment expectedFulfilment : expectedFulfilments) {
      boolean matchedExpectedFulfilment = false;
      
      for (int i=0; i<actualProducts.size(); i++) {
        Product candidateProduct = actualProducts.get(i); 
        if (candidateProduct.getType() == productType && expectedFulfilment.getPackCode().equals(candidateProduct.getPackCode())) {
           assertEquals(expectedFulfilment.getDescription(), candidateProduct.getDescription());
           assertEquals(expectedFulfilment.getMetadata(), candidateProduct.getMetadata());
           
           // Remove the current product from the list, to prevent it matching another fulfilment
           matchedExpectedFulfilment = true;
           actualProducts.remove(i);
           break;
        }
      }
      
      if (!matchedExpectedFulfilment) { 
        fail("No product for for Fulfilment. Type:" + productType + " PackCode:" + expectedFulfilment.getPackCode());
      }
    }
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

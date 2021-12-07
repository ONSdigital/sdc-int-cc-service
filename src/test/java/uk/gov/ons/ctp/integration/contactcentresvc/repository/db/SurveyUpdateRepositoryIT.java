package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.ProductGroup;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.event.SurveyUpdateEventReceiver;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

public class SurveyUpdateRepositoryIT extends PostgresTestBase {

  @Autowired private SurveyTransactionalOps txOps;

  @BeforeEach
  public void setup() throws Exception {
    txOps.deleteAll();

    // Load a random survey, to make that there is no inteference between the test survey
    // and any pre-existing surveys
    List<SurveyUpdateEvent> secondarySurveys =
        FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class, "Secondary");
    SurveyUpdateEvent secondarySurveyUpdateEvent = secondarySurveys.get(0);
    txOps.writeSurvey(secondarySurveyUpdateEvent);
  }

  @Test
  public void shouldLoadNewSurvey() throws Exception {
    // Load in the survey under test
    List<SurveyUpdateEvent> testSurveys =
        FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class);
    SurveyUpdateEvent surveyUpdateEvent = testSurveys.get(0);
    txOps.writeSurvey(surveyUpdateEvent);

    // Confirm survey+products stored correctly in db
    SurveyUpdate testSurvey = surveyUpdateEvent.getPayload().getSurveyUpdate();
    txOps.verifySurvey(testSurvey);
  }

  @Test
  public void surveyShouldUpdate() throws Exception {

    // Load in the survey under test
    List<SurveyUpdateEvent> testSurveys =
        FixtureHelper.loadClassFixtures(SurveyUpdateEvent[].class);
    SurveyUpdateEvent surveyUpdateEvent = testSurveys.get(0);
    txOps.writeSurvey(surveyUpdateEvent);

    // Confirm initial version stored correctly in db
    SurveyUpdate testSurvey = surveyUpdateEvent.getPayload().getSurveyUpdate();
    txOps.verifySurvey(testSurvey);

    // Update survey. Change the survey name
    testSurvey.setName("Hat size survey");

    // Update survey. Remove an existing fulfilment
    testSurvey.getAllowedSmsFulfilments().remove(0);

    // Update survey. Change an existing fulfilment
    SurveyFulfilment fulfilment = testSurvey.getAllowedPrintFulfilments().get(0);
    fulfilment.setDescription("N/A");
    fulfilment.getMetadata().put("code", "x11");

    // Update survey. Add a new fulfilment
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("NumberOfPages", "94");
    SurveyFulfilment newProduct =
        new SurveyFulfilment("replace-uac-sw", "Replacement UAC - Swahili", metadata);
    testSurvey.getAllowedPrintFulfilments().add(newProduct);

    // Write modified survey and make sure it's stored correctly in the db
    txOps.writeSurvey(surveyUpdateEvent);
    txOps.verifySurvey(testSurvey);
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class SurveyTransactionalOps {
    private CaseRepository caseRepo;
    private CollectionExerciseRepository collExRepo;
    private ProductRepository productRepo;
    private SurveyRepository repo;
    private CCSvcBeanMapper ccBeanMapper;

    public SurveyTransactionalOps(
        CaseRepository caseRepo,
        SurveyRepository repo,
        ProductRepository productRepo,
        CollectionExerciseRepository collExRepo,
        CCSvcBeanMapper ccBeanMapper) {
      this.repo = repo;
      this.caseRepo = caseRepo;
      this.collExRepo = collExRepo;
      this.productRepo = productRepo;
      this.ccBeanMapper = ccBeanMapper;
    }

    public void deleteAll() {
      caseRepo.deleteAll();
      collExRepo.deleteAll();
      productRepo.deleteAll();
      repo.deleteAll();
    }

    public void writeSurvey(SurveyUpdateEvent surveyUpdateEvent) throws Exception {
      SurveyUpdateEventReceiver surveyReceiver = new SurveyUpdateEventReceiver(repo, ccBeanMapper);
      surveyReceiver.acceptEvent(surveyUpdateEvent);
    }

    public void verifySurvey(SurveyUpdate expectedSurvey) {
      UUID surveyUUID = UUID.fromString(expectedSurvey.getSurveyId());
      Survey actualSurvey = repo.findById(surveyUUID).get();
      verifySurveysAreEqual(expectedSurvey, actualSurvey);
    }

    private void verifySurveysAreEqual(SurveyUpdate testSurvey, Survey loadedSurvey) {
      assertEquals(testSurvey.getSurveyId(), loadedSurvey.getId().toString());
      assertEquals(testSurvey.getName(), loadedSurvey.getName());
      assertEquals(testSurvey.getSampleDefinitionUrl(), loadedSurvey.getSampleDefinitionUrl());
      assertEquals(testSurvey.getMetadata(), loadedSurvey.getMetadata());

      // Verify that the number of loaded fulfilments is correct
      int expectedNumFulfilments =
          testSurvey.getAllowedPrintFulfilments().size()
              + testSurvey.getAllowedSmsFulfilments().size()
              + testSurvey.getAllowedEmailFulfilments().size();
      List<Product> actualFulfilments = new ArrayList<>(loadedSurvey.getAllowedFulfilments());
      assertEquals(expectedNumFulfilments, actualFulfilments.size());

      // Confirm products loaded
      verifyProductsLoaded(
          DeliveryChannel.POST, testSurvey.getAllowedPrintFulfilments(), actualFulfilments);
      verifyProductsLoaded(
          DeliveryChannel.SMS, testSurvey.getAllowedSmsFulfilments(), actualFulfilments);
      verifyProductsLoaded(
          DeliveryChannel.EMAIL, testSurvey.getAllowedEmailFulfilments(), actualFulfilments);
    }

    private void verifyProductsLoaded(
        DeliveryChannel deliveryChannel,
        List<SurveyFulfilment> expectedFulfilments,
        List<Product> actualProducts) {

      for (SurveyFulfilment expectedFulfilment : expectedFulfilments) {
        boolean foundFulfilment = false;

        for (int i = 0; i < actualProducts.size(); i++) {
          Product candidateProduct = actualProducts.get(i);
          if (candidateProduct.getDeliveryChannel() == deliveryChannel
              && expectedFulfilment.getPackCode().equals(candidateProduct.getPackCode())) {
            assertEquals(expectedFulfilment.getDescription(), candidateProduct.getDescription());
            assertEquals(expectedFulfilment.getMetadata(), candidateProduct.getMetadata());
            assertEquals(ProductGroup.UAC, candidateProduct.getProductGroup());

            foundFulfilment = true;
            break;
          }
        }

        if (!foundFulfilment) {
          fail(
              "No product found for for Fulfilment. "
                  + "Type:"
                  + deliveryChannel
                  + " PackCode:"
                  + expectedFulfilment.getPackCode());
        }
      }
    }
  }
}

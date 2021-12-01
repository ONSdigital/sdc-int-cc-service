package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@Slf4j
@MessageEndpoint
public class SurveyEventReceiver {
  private SurveyRepository repo;
  private MapperFacade mapper;

  public SurveyEventReceiver(SurveyRepository repo, MapperFacade mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  /**
   * Receive and process Collection Exercise events.
   *
   * @param event event from RM.
   * @throws Exception 
   */
  @ServiceActivator(inputChannel = "acceptSurveyUpdateEvent")
  @Transactional
  public void acceptEvent(SurveyUpdateEvent event) throws Exception {

    SurveyUpdate payload = event.getPayload().getSurveyUpdate();
    
    log.info(
        "Entering acceptSurveyUpdateEvent",
        kv("messageId", event.getHeader().getMessageId()),
        kv("surveyId", payload.getSurveyId()));

    try {
      Survey survey = mapper.map(payload, Survey.class);

//      Map<String, ?> m = survey.getAllowedPrintFulfilments().get(0).getMetadata();
//
//      String valueClassName = m.get("suitableRegions").getClass().getCanonicalName();
//      ArrayList<String> foo = (ArrayList<String>) m.get("suitableRegions");
      
      survey.setSampleDefinition(payload.getSampleDefinition());
      
      setSurveyIdForProducts(survey.getAllowedPrintFulfilments(), survey);
      setSurveyIdForProducts(survey.getAllowedSmsFulfilments(), survey);
      setSurveyIdForProducts(survey.getAllowedEmailFulfilments(), survey);

      setProductType(survey.getAllowedPrintFulfilments(), ProductType.POSTAL);
      setProductType(survey.getAllowedSmsFulfilments(), ProductType.SMS);
      setProductType(survey.getAllowedEmailFulfilments(), ProductType.EMAIL);
      
      repo.saveAndFlush(survey);
    } catch (Exception e) {
      log.error(
          "Survey Event processing failed", kv("messageId", event.getHeader().getMessageId()), e);
      throw e;
    }
  }

  private void setProductType(List<Product> products, ProductType productType) throws IOException {
    if (products != null) {
      for (Product product : products) {
        product.setType(productType);
        product.serializeMetadata();
      }
    }
    
  }

  private void setSurveyIdForProducts(List<Product> products, Survey survey) {
    if (products != null) {
      for (Product product : products) {
        product.setSurvey(survey);
      }
    }
  }
}

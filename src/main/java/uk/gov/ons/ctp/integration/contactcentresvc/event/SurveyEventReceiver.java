package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.model.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductGroup;
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
  public void acceptEvent(SurveyUpdateEvent event) {

    SurveyUpdate payload = event.getPayload().getSurveyUpdate();

    log.info(
        "Entering acceptSurveyUpdateEvent",
        kv("messageId", event.getHeader().getMessageId()),
        kv("surveyId", payload.getSurveyId()));

    try {
      Survey survey = mapper.map(payload, Survey.class);

      // Squash the 3 fulfilment collections into a single list
      List<Product> buildFulfilmentList = new ArrayList<Product>();
      addProductsToList(
          buildFulfilmentList, DeliveryChannel.POST, payload.getAllowedPrintFulfilments(), survey);
      addProductsToList(
          buildFulfilmentList, DeliveryChannel.SMS, payload.getAllowedSmsFulfilments(), survey);
      addProductsToList(
          buildFulfilmentList, DeliveryChannel.EMAIL, payload.getAllowedEmailFulfilments(), survey);

      survey.setAllowedFulfilments(buildFulfilmentList);

      repo.saveAndFlush(survey);
    } catch (Exception e) {
      log.error(
          "Survey Event processing failed", kv("messageId", event.getHeader().getMessageId()), e);
      throw e;
    }
  }

  private void addProductsToList(
      List<Product> allowedFulfilments,
      DeliveryChannel deliveryChannel,
      List<SurveyFulfilment> fulfilments,
      Survey survey) {

    if (fulfilments != null) {
      for (SurveyFulfilment fulfilment : fulfilments) {
        Product product = mapper.map(fulfilment, Product.class);

        product.setSurvey(survey);
        product.setDeliveryChannel(deliveryChannel);
        product.setProductGroup(ProductGroup.UAC);

        allowedFulfilments.add(product);
      }
    }
  }
}

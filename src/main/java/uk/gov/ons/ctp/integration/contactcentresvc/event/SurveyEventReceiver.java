package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
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
   */
  @ServiceActivator(inputChannel = "acceptSurveyUpdateEvent")
  public void acceptEvent(SurveyUpdateEvent event) {

    SurveyUpdate payload = event.getPayload().getSurveyUpdate();

    log.info(
        "Entering acceptSurveyUpdateEvent",
        kv("messageId", event.getHeader().getMessageId()),
        kv("surveyId", payload.getSurveyId()));

    try {
      Survey survey = mapper.map(payload, Survey.class);
      repo.save(survey);
    } catch (Exception e) {
      log.error(
          "Survey Event processing failed", kv("messageId", event.getHeader().getMessageId()), e);
      throw e;
    }
  }
}

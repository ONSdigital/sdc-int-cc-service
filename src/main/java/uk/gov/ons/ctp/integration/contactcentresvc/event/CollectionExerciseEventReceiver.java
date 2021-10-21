package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;

/** Service implementation responsible for receipt of Collection Exercise Events. */
@Slf4j
@MessageEndpoint
public class CollectionExerciseEventReceiver {

  private CollectionExerciseRepository repo;
  private MapperFacade mapper;

  public CollectionExerciseEventReceiver(CollectionExerciseRepository repo, MapperFacade mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  /**
   * Receive and process Collection Exercise events.
   *
   * @param event event from RM
   */
  @ServiceActivator(inputChannel = "acceptCollectionExerciseEvent")
  public void acceptEvent(CollectionExerciseUpdateEvent event) {

    var payload = event.getPayload().getCollectionExerciseUpdate();

    log.info(
        "Entering acceptCollectionExerciseUpdateEvent",
        kv("messageId", event.getHeader().getMessageId()),
        kv("collectionExerciseId", payload.getCollectionExerciseId()),
        kv("surveyId", payload.getSurveyId()));

    try {
      CollectionExercise entity = mapper.map(payload, CollectionExercise.class);
      repo.save(entity);
    } catch (Exception e) {
      log.error(
          "CollectionExercise Event processing failed",
          kv("messageId", event.getHeader().getMessageId()),
          e);
      throw e;
    }
  }
}

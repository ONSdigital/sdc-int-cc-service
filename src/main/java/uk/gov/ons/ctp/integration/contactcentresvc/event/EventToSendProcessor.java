package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@Slf4j
@Component
public class EventToSendProcessor {
  private final EventToSendRepository eventToSendRepository;
  private final EventPublisher eventPublisher;
  private final AppConfig appConfig;

  @Value("${scheduler.chunk-size}")
  private int chunkSize;

  public EventToSendProcessor(
      EventToSendRepository repo, EventPublisher eventPublisher, AppConfig appConfig) {
    this.eventToSendRepository = repo;
    this.eventPublisher = eventPublisher;
    this.appConfig = appConfig;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every chunk
  public int processChunk() {
    AtomicInteger numProcessed = new AtomicInteger();
    try (Stream<EventToSend> events = eventToSendRepository.findEventsToSend(chunkSize)) {
      List<EventToSend> eventsSent = new ArrayList<>();
      events.forEach(
          event -> {
            numProcessed.incrementAndGet();
            try {
              TopicType type = TopicType.valueOf(event.getType());
              UUID messageId =
                  eventPublisher.sendEvent(
                      type, Source.CONTACT_CENTRE_API, appConfig.getChannel(), event.getPayload());

              eventsSent.add(event);
              log.info("Event published", kv("messageId", messageId));
            } catch (Exception e) {
              log.error("Could not send event. Will retry indefinitely", kv("event", event), e);
            }
          });

      eventToSendRepository.deleteAllInBatch(eventsSent);
    }
    return numProcessed.get();
  }
}

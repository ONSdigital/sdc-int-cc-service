package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public class EventToSendRepositoryTest extends PostgresTestBase {

  @Autowired private EventToSendRepository repo;

  @Test
  public void shouldFindSingleEvent() {
    LocalDateTime now = LocalDateTime.now();
    EventToSend event =
        EventToSend.builder()
            .id(UUID.randomUUID())
            .payload("payload")
            .type(TopicType.SURVEY_LAUNCH.name())
            .createdDateTime(now)
            .build();

    repo.save(event);
    repo.findEventsToSend(3);

    AtomicInteger numProcessed = new AtomicInteger();

    try (Stream<EventToSend> events = repo.findEventsToSend(3)) {
      events.forEach(
          ev -> {
            numProcessed.incrementAndGet();
          });
    }
    assertEquals(1, numProcessed.get());
  }
}

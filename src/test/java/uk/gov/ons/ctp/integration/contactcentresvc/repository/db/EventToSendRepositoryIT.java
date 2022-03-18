package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public class EventToSendRepositoryIT extends PostgresTestBase {

  @Autowired private EventToSendRepository repo;
  @Autowired private TransactionalOps txOps;

  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }

  @Test
  public void shouldFindSingleEvent() {
    LocalDateTime now = LocalDateTime.now();
    txOps.createEvent(now);

    AtomicInteger numProcessed = new AtomicInteger();

    try (Stream<EventToSend> events = repo.findEventsToSend(3)) {
      events.forEach(
          ev -> {
            numProcessed.incrementAndGet();
          });
    }
    assertEquals(1, numProcessed.get());
  }

  @Test
  public void shouldFindEventsInTimeOrder() {
    LocalDateTime t1 = parseDateTime("2022-09-06T09:37:52.728");
    LocalDateTime t2 = parseDateTime("2022-09-05T09:37:52.728");
    LocalDateTime t3 = parseDateTime("2022-09-06T09:37:53.000");
    LocalDateTime t4 = parseDateTime("2022-09-06T09:30:52.728");
    // time order: t2, t4, t1, t3

    txOps.createEvent(t1);
    txOps.createEvent(t2);
    txOps.createEvent(t3);
    txOps.createEvent(t4);

    var items = repo.findEventsToSend(200).collect(Collectors.toList());
    assertEquals(4, items.size());

    // verify they are received in time order
    assertEquals(t2, items.get(0).getCreatedDateTime());
    assertEquals(t4, items.get(1).getCreatedDateTime());
    assertEquals(t1, items.get(2).getCreatedDateTime());
    assertEquals(t3, items.get(3).getCreatedDateTime());
  }

  @Test
  public void shouldLimitEvents() {
    IntStream.range(0, 10)
        .forEach(
            i -> {
              txOps.createEvent(LocalDateTime.now());
            });

    assertEquals(10, repo.count());

    var items = repo.findEventsToSend(3).collect(Collectors.toList());
    assertEquals(3, items.size());
  }

  private LocalDateTime parseDateTime(String dateTime) {
    return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}

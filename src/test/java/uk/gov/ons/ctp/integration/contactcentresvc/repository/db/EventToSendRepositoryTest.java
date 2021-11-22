package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendPoller;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test-containers-flyway")
@Testcontainers
@TestPropertySource(properties = {"GOOGLE_CLOUD_PROJECT=sdc-cc-test"})
@MockBean({EventToSendPoller.class, EventPublisher.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
@Transactional
public class EventToSendRepositoryTest {

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

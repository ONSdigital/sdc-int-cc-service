package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public class EventToSendRepositoryIT2 extends PostgresTestBase {

  @Autowired private EventToSendRepository repo;
  @Autowired private SurveyRepository repo2;
  @Autowired private TransactionalOps2 txOps;

  {
    System.out.println("-------------------- starting ");
  }
  
  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }

  @Test
  public void shouldLimitEvents() {
    assertEquals(10, 9);
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps2 {
    private EventToSendRepository repo;

    public TransactionalOps2(EventToSendRepository repo) {
      this.repo = repo;
    }

    public void deleteAll() {
      repo.deleteAll();
    }

    public void createEvent(LocalDateTime created) {
      EventToSend event =
          EventToSend.builder()
              .id(UUID.randomUUID())
              .payload("payload")
              .type(TopicType.SURVEY_LAUNCH.name())
              .createdDateTime(created)
              .build();

      repo.save(event);
    }
  }
}

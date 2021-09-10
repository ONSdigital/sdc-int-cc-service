package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public interface EventToSendRepository extends JpaRepository<EventToSend, UUID> {
  @Query(
      value = "SELECT * FROM cc_schema.event_to_send LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Stream<EventToSend> findEventsToSend(@Param("limit") int limit);
}

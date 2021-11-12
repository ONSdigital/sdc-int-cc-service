package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public interface EventToSendRepository extends JpaRepository<EventToSend, UUID> {
  /**
   * Find a chunk of work-items (events to send) and "claim" them at DB level using postgres SQL
   * extension "UPDATE SKIP LOCKED" facility which is designed for this type of work-queue
   * processing.
   *
   * <p>A useful description of the postgres query is here:
   * https://www.2ndquadrant.com/en/blog/what-is-select-skip-locked-for-in-postgresql-9-5/
   *
   * @param limit the maximum number of items to return
   * @return a stream of <code>EventToSend</code> items that had previously not been claimed for
   *     processing, but now they are claimed and locked for this transaction context, ordered by
   *     created_date_time
   */
  @Query(
      value =
          "SELECT * FROM cc_schema.event_to_send ORDER BY "
              + "cc_schema.event_to_send.created_date_time LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Stream<EventToSend> findEventsToSend(@Param("limit") int limit);
}

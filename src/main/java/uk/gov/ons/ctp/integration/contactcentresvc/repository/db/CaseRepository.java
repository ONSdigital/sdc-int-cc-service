package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;

public interface CaseRepository extends JpaRepository<Case, UUID> {
  Optional<Case> findByCaseRef(String caseRef);

  @Query(
      value =
          "SELECT * FROM collection_case WHERE sample ->> ((:key, ' ', '')) LIKE CONCAT('%', UPPER(REPLACE(:value, ' ', '')), '%')",
      nativeQuery = true)
  List<Case> findBySampleContains(@Param("key") String key, @Param("value") String value);
}

package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;

public interface CaseRepository extends JpaRepository<Case, UUID> {
  Optional<Case> findByCaseRef(String caseRef);

  @Query(
      value =
          "SELECT * FROM cc_schema.collection_case WHERE UPPER(REPLACE(sample ->> ?1, ' ', '')) "
              + "LIKE CONCAT('%', UPPER(REPLACE(?2, ' ', '')), '%')",
      nativeQuery = true)
  List<Case> findBySampleContains(String key, String value);
}

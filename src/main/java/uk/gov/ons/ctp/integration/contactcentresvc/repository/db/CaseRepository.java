package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;

public interface CaseRepository extends JpaRepository<Case, UUID> {
  Optional<Case> findByCaseRef(String caseRef);

  List<Case> findByAddressUprn(String uprn);
}

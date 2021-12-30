package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;

public interface UacRepository extends JpaRepository<Uac, UUID> {

  List<Uac> findByCaseId(UUID caseId);
}

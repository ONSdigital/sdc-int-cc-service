package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.OperatorRole;

public interface OperatorRoleRepository extends JpaRepository<OperatorRole, UUID> {}

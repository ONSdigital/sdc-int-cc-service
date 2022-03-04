package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.UserAudit;

@Repository
public interface UserAuditRepository extends JpaRepository<UserAudit, UUID> {

  int countAllByCcuserIdAndAuditType(UUID ccUserId, AuditType auditType);
}

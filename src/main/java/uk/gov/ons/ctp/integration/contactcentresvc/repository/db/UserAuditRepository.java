package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.UserAudit;

@Repository
public interface UserAuditRepository extends JpaRepository<UserAudit, UUID> {

  int countAllByCcuserIdAndAuditType(UUID ccUserId, AuditType auditType);

  List<UserAudit> findAllByCcuserId(UUID identity);

  List<UserAudit> findAllByTargetUserId(UUID identity);

  boolean existsByCcuserIdAndAuditType(UUID ccUserId, AuditType auditType);
}

package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.UserAudit;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;

@Slf4j
@Service
public class UserAuditService {

  @Autowired UserAuditRepository userAuditRepository;

  public void saveUserAudit(
      UUID principalId,
      UUID userId,
      UUID roleId,
      AuditType auditType,
      AuditSubType auditSubType,
      String value) {

    // TODO verify

    UserAudit userAudit =
        UserAudit.builder()
            .id(principalId)
            .targetUserId(userId)
            .targetRoleId(roleId)
            .auditType(auditType)
            .auditSubType(auditSubType)
            .auditValue(value)
            .build();

    userAuditRepository.saveAndFlush(userAudit);
  }
}

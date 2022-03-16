package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditTarget;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.UserAudit;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@Service
public class UserAuditService {

  @Autowired UserAuditRepository userAuditRepository;
  @Autowired UserRepository userRepository;
  @Autowired RoleRepository roleRepository;

  public void saveUserAudit(
      String targetUserName,
      String targetRoleName,
      AuditType auditType,
      AuditSubType auditSubType,
      String value)
      throws CTPException {

    UUID principalId = lookupUserId(UserIdentityContext.get());
    UUID targetUserId = null;
    UUID targetRoleId = null;

    if (targetUserName != null) {
      targetUserId = lookupUserId(targetUserName);
    }
    if (targetRoleName != null) {
      targetRoleId =
          roleRepository
              .findByName(targetRoleName)
              .orElseThrow(() -> new CTPException(CTPException.Fault.BAD_REQUEST, "Role not found"))
              .getId();
    }

    UserAudit userAudit =
        UserAudit.builder()
            .ccuserId(principalId)
            .targetUserId(targetUserId)
            .targetRoleId(targetRoleId)
            .auditType(auditType)
            .auditSubType(auditSubType)
            .auditValue(value)
            .createdDateTime(LocalDateTime.now())
            .build();

    verifyAudit(userAudit);

    userAuditRepository.saveAndFlush(userAudit);
  }

  private void verifyAudit(UserAudit userAudit) throws CTPException {

    Set<AuditSubType> validSubTypes = userAudit.getAuditType().getValidSubTypes();
    boolean subTypeUsed = !validSubTypes.isEmpty() || userAudit.getAuditSubType() != null;
    if (subTypeUsed && !validSubTypes.contains(userAudit.getAuditSubType())) {
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Unexpected AuditSubType for given AuditType");
    }

    if (Arrays.asList(userAudit.getAuditType().getTargets()).contains(AuditTarget.USER)) {
      if (userAudit.getTargetUserId() == null) {
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Target userId not supplied");
      }
    } else {
      if (userAudit.getTargetUserId() != null) {
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Target userId supplied but not expected");
      }
    }

    if (Arrays.asList(userAudit.getAuditType().getTargets()).contains(AuditTarget.ROLE)) {
      if (userAudit.getTargetRoleId() == null) {
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Target roleId not supplied");
      }
    } else {
      if (userAudit.getTargetRoleId() != null) {
        throw new CTPException(
            CTPException.Fault.SYSTEM_ERROR, "Target roleId supplied but not expected");
      }
    }
  }

  public List<UserAudit> getAuditHistoryForPerformedBy(String principle) throws CTPException {

    UUID principleUserId = lookupUserId(principle);
    List<UserAudit> auditHistory = userAuditRepository.findAllByCcuserId(principleUserId);

    return auditHistory;
  }

  public List<UserAudit> getAuditHistoryForPerformedOn(String targetUser) throws CTPException {

    UUID targetUserId = lookupUserId(targetUser);
    List<UserAudit> auditHistory = userAuditRepository.findAllByTargetUserId(targetUserId);

    return auditHistory;
  }

  private UUID lookupUserId(String userIdentity) throws CTPException {
    UUID targetUserId =
        userRepository
            .findByIdentity(userIdentity)
            .orElseThrow(
                () ->
                    new CTPException(
                        CTPException.Fault.BAD_REQUEST, "User not found: " + userIdentity))
            .getId();

    return targetUserId;
  }
}

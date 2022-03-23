package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.logging.log4j.util.Strings;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;

@Service
public class UserAuditService {

  @Autowired private RBACService rbacService;
  @Autowired private UserService userService;

  @Autowired UserAuditRepository userAuditRepository;
  @Autowired UserRepository userRepository;
  @Autowired RoleRepository roleRepository;

  @Autowired private MapperFacade mapper;

  public void saveUserAudit(
      String targetUserName,
      String targetRoleName,
      AuditType auditType,
      AuditSubType auditSubType,
      String value)
      throws CTPException {

    UUID principalId = getUserIdByIdentity(UserIdentityContext.get());
    UUID targetUserId = null;
    UUID targetRoleId = null;

    if (targetUserName != null) {
      targetUserId = getUserIdByIdentity(targetUserName);
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

  public List<UserAuditDTO> searchAuditHistory(String performedBy, String performedOn)
      throws CTPException {

    // This is a fairly simple algorithm and could benefit from upgrading to a pure JPA solution.
    // In the meanwhile performance is improved by using a cache to hold the results of frequent
    // name/role
    // lookups. It tends to be the same set of small originating data, so this simple change makes a
    // huge improvement to the run time. eg, Request time for about 200 audit records which was
    // 800ms drops to 45ms (empty response is 38ms).
    Map<UUID, String> userNameCache = new HashedMap<>();
    Map<UUID, String> roleNameCache = new HashedMap<>();

    // Search the audit table
    List<UserAudit> auditHistory;
    if (Strings.isNotBlank(performedBy)) {
      UUID performedByUser = getUserIdByIdentity(performedBy);
      auditHistory = userAuditRepository.findAllByCcuserId(performedByUser);
    } else {
      UUID performedOnUser = getUserIdByIdentity(performedOn);
      auditHistory = userAuditRepository.findAllByTargetUserId(performedOnUser);
    }

    // Convert results to the response type
    List<UserAuditDTO> auditHistoryResponse = new ArrayList<>();
    for (UserAudit userAudit : auditHistory) {
      UserAuditDTO userAuditDTO = mapper.map(userAudit, UserAuditDTO.class);
      userAuditDTO.setPerformedByUser(getUserIdentityById(userNameCache, userAudit.getCcuserId()));
      userAuditDTO.setPerformedOnUser(
          getUserIdentityById(userNameCache, userAudit.getTargetUserId()));
      userAuditDTO.setRoleName(getRoleNameById(roleNameCache, userAudit.getTargetRoleId()));
      auditHistoryResponse.add(userAuditDTO);
    }

    // Sort from newest to oldest
    auditHistoryResponse.sort(Comparator.comparing(UserAuditDTO::getCreatedDateTime).reversed());
    return auditHistoryResponse;
  }

  private String getRoleNameById(Map<UUID, String> roleNameCache, UUID roleId) throws CTPException {

    // Attempt to get name from cache
    if (roleNameCache.containsKey(roleId)) {
      return roleNameCache.get(roleId);
    }

    String roleName = rbacService.getRoleNameForId(roleId);
    roleNameCache.put(roleId, roleName);

    return roleName;
  }

  private UUID getUserIdByIdentity(String userIdentity) throws CTPException {

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

  // Null tolerant method to get the name of a user
  private String getUserIdentityById(Map<UUID, String> userNameCache, UUID userUUID)
      throws CTPException {
    if (userUUID == null) {
      return null;
    }

    // Attempt to get name from cache
    if (userNameCache.containsKey(userUUID)) {
      return userNameCache.get(userUUID);
    }

    String userIdentity = userService.getUserIdentity(userUUID);
    userNameCache.put(userUUID, userIdentity);

    return userIdentity;
  }
}

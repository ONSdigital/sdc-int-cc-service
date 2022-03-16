package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import com.google.api.client.util.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.UserAudit;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyUserRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.UserAuditService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.UserService;

@Slf4j
@RestController
@Validated
@RequestMapping(value = "/users", produces = "application/json")
public class UserEndpoint {

  private RBACService rbacService;
  private UserService userService;
  private UserAuditService userAuditService;

  @Autowired private MapperFacade mapper;

  /**
   * Create the endpoint
   *
   * @param rbacService used for
   * @param userService
   */
  @Autowired
  public UserEndpoint(
      final RBACService rbacService,
      final UserService userService,
      final UserAuditService userAuditService) {
    this.rbacService = rbacService;
    this.userService = userService;
    this.userAuditService = userAuditService;
  }

  @PutMapping("/login")
  public ResponseEntity<UserDTO> login(@RequestBody LoginRequestDTO loginRequestDTO)
      throws CTPException {

    log.info(
        "Entering login",
        kv("forename", loginRequestDTO.getForename()),
        kv("surname", loginRequestDTO.getSurname()));

    rbacService.assertUserValidAndActive();

    String userIdentity = UserIdentityContext.get();

    userAuditService.saveUserAudit(userIdentity, null, AuditType.LOGIN, null, null);

    UserDTO userDTO = userService.getUser(userIdentity);

    // Update users name
    userDTO.setForename(loginRequestDTO.getForename());
    userDTO.setSurname(loginRequestDTO.getSurname());

    userService.modifyUser(userDTO);

    return ResponseEntity.ok(userDTO);
  }

  @PutMapping("/logout")
  public ResponseEntity<Void> logout() throws CTPException {

    log.info("Entering logout");

    rbacService.assertUserValidAndActive();

    String userIdentity = UserIdentityContext.get();
    userAuditService.saveUserAudit(userIdentity, null, AuditType.LOGOUT, null, null);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/{userIdentity}")
  public ResponseEntity<UserDTO> getUserByName(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity) throws CTPException {

    log.info("Entering getUserByName", kv("userIdentity", userIdentity));
    rbacService.assertUserPermission(PermissionType.READ_USER);

    return ResponseEntity.ok(userService.getUser(userIdentity));
  }

  @GetMapping("/permissions")
  public ResponseEntity<Set<PermissionType>> getLoggedInUsersPermissions() throws CTPException {

    String userIdentity = UserIdentityContext.get();
    log.info("Entering getLoggedInUsersPermissions", kv("userIdentity", userIdentity));

    rbacService.assertUserValidAndActive();

    // All users need to access this so no permission assertion
    List<RoleDTO> usersRoles = userService.getUsersRoles(userIdentity);
    if (usersRoles.isEmpty()) {
      throw new CTPException(Fault.ACCESS_DENIED, "User has no roles assigned");
    }
    return ResponseEntity.ok(
        usersRoles.stream()
            .map(r -> r.getPermissions())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet()));
  }

  @GetMapping
  public ResponseEntity<List<UserDTO>> getUsers() throws CTPException {
    log.info("Entering getUsers");
    rbacService.assertUserPermission(PermissionType.READ_USER);

    List<UserDTO> users = userService.getUsers();

    List<UserDTO> sortedUsers =
        users.stream()
            .sorted(Comparator.comparing(UserDTO::getIdentity))
            .collect(Collectors.toList());

    return ResponseEntity.ok(sortedUsers);
  }

  @PutMapping("/{userIdentity}")
  public ResponseEntity<UserDTO> modifyUser(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @RequestBody ModifyUserRequestDTO modifyUserRequestDTO)
      throws CTPException {

    log.info(
        "Entering modifyUser",
        kv("userIdentity", userIdentity),
        kv("active", modifyUserRequestDTO.isActive()));

    rbacService.assertUserPermission(PermissionType.MODIFY_USER);
    rbacService.assertNotSelfModification(userIdentity);

    // Fetch and update the user record
    UserDTO userDTO = userService.getUser(userIdentity);
    userDTO.setActive(modifyUserRequestDTO.isActive());
    UserDTO updatedUser = userService.modifyUser(userDTO);

    // User audit
    String status = userDTO.isActive() ? "ACTIVE" : "INACTIVE";
    userAuditService.saveUserAudit(
        userIdentity, null, AuditType.USER, AuditSubType.MODIFIED, status);

    return ResponseEntity.ok(updatedUser);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws CTPException {
    log.info("Entering createUser", kv("userIdentity", userDTO.getIdentity()));

    rbacService.assertUserPermission(PermissionType.CREATE_USER);

    UserDTO createdUser = userService.createUser(userDTO);

    userAuditService.saveUserAudit(
        createdUser.getIdentity(), null, AuditType.USER, AuditSubType.CREATED, null);

    return ResponseEntity.ok(createdUser);
  }

  @Transactional
  @DeleteMapping("/{userIdentity}")
  public ResponseEntity<UserDTO> deleteUser(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity) throws CTPException {
    log.info("Entering deleteUser", kv("userIdentity", userIdentity));

    rbacService.assertUserPermission(PermissionType.DELETE_USER);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(userIdentity, null, AuditType.USER, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.deleteUser(userIdentity));
  }

  @PatchMapping("/{userIdentity}/addSurvey/{surveyType}")
  public ResponseEntity<UserDTO> addUserSurvey(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "surveyType") SurveyType surveyType)
      throws CTPException {
    log.info(
        "Entering addUserSurvey", kv("userIdentity", userIdentity), kv("surveyType", surveyType));

    rbacService.assertUserPermission(PermissionType.USER_SURVEY_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, null, AuditType.USER_SURVEY_USAGE, AuditSubType.ADDED, surveyType.name());

    return ResponseEntity.ok(userService.addUserSurvey(userIdentity, surveyType));
  }

  @PatchMapping("/{userIdentity}/removeSurvey/{surveyType}")
  public ResponseEntity<UserDTO> removeUserSurvey(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "surveyType") SurveyType surveyType)
      throws CTPException {
    log.info(
        "Entering removeUserSurvey",
        kv("userIdentity", userIdentity),
        kv("surveyType", surveyType));

    rbacService.assertUserPermission(PermissionType.USER_SURVEY_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, null, AuditType.USER_SURVEY_USAGE, AuditSubType.REMOVED, surveyType.name());

    return ResponseEntity.ok(userService.removeUserSurvey(userIdentity, surveyType));
  }

  @PatchMapping("/{userIdentity}/addUserRole/{roleName}")
  public ResponseEntity<UserDTO> addUserRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.USER_ROLE, AuditSubType.ADDED, null);

    return ResponseEntity.ok(userService.addUserRole(userIdentity, roleName));
  }

  @PatchMapping("/{userIdentity}/removeUserRole/{roleName}")
  public ResponseEntity<UserDTO> removeUserRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {
    log.info("Entering removeUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));

    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.USER_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.removeUserRole(userIdentity, roleName));
  }

  @PatchMapping("/{userIdentity}/addAdminRole/{roleName}")
  public ResponseEntity<UserDTO> addAdminRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.ADMIN_ROLE, AuditSubType.ADDED, null);

    return ResponseEntity.ok(userService.addAdminRole(userIdentity, roleName));
  }

  @PatchMapping("/{userIdentity}/removeAdminRole/{roleName}")
  public ResponseEntity<UserDTO> removeAdminRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info(
        "Entering removeAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.ADMIN_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.removeAdminRole(userIdentity, roleName));
  }

  @GetMapping("/audit")
  public ResponseEntity<List<UserAuditDTO>> audit(
      @RequestParam(required = false) String performedBy,
      @RequestParam(required = false) String performedOn)
      throws CTPException {

    log.info(
        "Entering audit search", kv("performedBy", performedBy), kv("performedOn", performedOn));

    // Verify that the caller can retrieve audit history
    rbacService.assertUserPermission(PermissionType.READ_USER_AUDIT);

    // Search the audit table
    List<UserAudit> auditHistory;
    if (performedBy != null && Strings.isNullOrEmpty(performedOn)) {
      auditHistory = userAuditService.getAuditHistoryForPerformedBy(performedBy);
    } else if (performedOn != null && Strings.isNullOrEmpty(performedBy)) {
      auditHistory = userAuditService.getAuditHistoryForPerformedOn(performedOn);
    } else {
      throw new CTPException(
          Fault.BAD_REQUEST, "Only one of 'performedByUser' or 'performedOnUser' must be supplied");
    }

    // Convert results to the response type
    List<UserAuditDTO> auditHistoryResponse = new ArrayList<>();
    for (UserAudit userAudit : auditHistory) {
      UserAuditDTO userAuditDTO = mapper.map(userAudit, UserAuditDTO.class);
      userAuditDTO.setPerformedByUser(getUser(userAudit.getCcuserId()));
      userAuditDTO.setPerformedOnUser(getUser(userAudit.getTargetUserId()));
      userAuditDTO.setRoleName(rbacService.getRoleNameForId(userAudit.getTargetRoleId()));
      auditHistoryResponse.add(userAuditDTO);
    }

    // Sort from newest to oldest
    auditHistoryResponse.sort(Comparator.comparing(UserAuditDTO::getCreatedDateTime).reversed());

    return ResponseEntity.ok(auditHistoryResponse);
  }

  // Null tolerant method to get the name of a user
  private String getUser(UUID userUUID) throws CTPException {
    if (userUUID == null) {
      return null;
    }

    return userService.getUserIdentity(userUUID);
  }
}

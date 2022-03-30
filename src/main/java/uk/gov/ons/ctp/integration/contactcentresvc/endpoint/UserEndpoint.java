package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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

  /**
   * Create the endpoint
   *
   * @param rbacService used for
   * @param userService
   */
  public UserEndpoint(
      final RBACService rbacService,
      final UserService userService,
      final UserAuditService userAuditService) {
    this.rbacService = rbacService;
    this.userService = userService;
    this.userAuditService = userAuditService;
  }

  @PutMapping("/login")
  @Transactional
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

  // TODO
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

  @Transactional
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

  @Transactional
  @PostMapping
  public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws CTPException {
    log.info("Entering createUser", kv("userIdentity", userDTO.getIdentity()));

    rbacService.assertUserPermission(PermissionType.CREATE_USER);

    UserDTO createdUser = userService.createUser(userDTO);

    userAuditService.saveUserAudit(
        createdUser.getIdentity(), null, AuditType.USER, AuditSubType.CREATED, null);

    return ResponseEntity.ok(createdUser);
  }

  // TODO
  @Transactional
  @DeleteMapping("/{userIdentity}")
  public ResponseEntity<UserDTO> deleteUser(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity) throws CTPException {
    log.info("Entering deleteUser", kv("userIdentity", userIdentity));

    rbacService.assertUserPermission(PermissionType.DELETE_USER);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.deleteUser(userIdentity);

    userAuditService.saveUserAudit(userIdentity, null, AuditType.USER, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(response);
  }

  @Transactional
  @PatchMapping("/{userIdentity}/addSurvey/{surveyType}")
  public ResponseEntity<UserDTO> addUserSurvey(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "surveyType") SurveyType surveyType)
      throws CTPException {
    log.info(
        "Entering addUserSurvey", kv("userIdentity", userIdentity), kv("surveyType", surveyType));

    rbacService.assertUserPermission(PermissionType.USER_SURVEY_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.addUserSurvey(userIdentity, surveyType);

    userAuditService.saveUserAudit(
        userIdentity, null, AuditType.USER_SURVEY_USAGE, AuditSubType.ADDED, surveyType.name());

    return ResponseEntity.ok(response);
  }

  @Transactional
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

    UserDTO response = userService.removeUserSurvey(userIdentity, surveyType);

    userAuditService.saveUserAudit(
        userIdentity, null, AuditType.USER_SURVEY_USAGE, AuditSubType.REMOVED, surveyType.name());

    return ResponseEntity.ok(response);
  }

  @Transactional
  @PatchMapping("/{userIdentity}/addUserRole/{roleName}")
  public ResponseEntity<UserDTO> addUserRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.addUserRole(userIdentity, roleName);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.USER_ROLE, AuditSubType.ADDED, null);

    return ResponseEntity.ok(response);
  }

  @Transactional
  @PatchMapping("/{userIdentity}/removeUserRole/{roleName}")
  public ResponseEntity<UserDTO> removeUserRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {
    log.info("Entering removeUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));

    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.removeUserRole(userIdentity, roleName);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.USER_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(response);
  }

  @Transactional
  @PatchMapping("/{userIdentity}/addAdminRole/{roleName}")
  public ResponseEntity<UserDTO> addAdminRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.addAdminRole(userIdentity, roleName);
    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.ADMIN_ROLE, AuditSubType.ADDED, null);

    return ResponseEntity.ok(response);
  }

  @Transactional
  @PatchMapping("/{userIdentity}/removeAdminRole/{roleName}")
  public ResponseEntity<UserDTO> removeAdminRole(
      @PathVariable(value = "userIdentity") @Valid @Email String userIdentity,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info(
        "Entering removeAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userIdentity);

    UserDTO response = userService.removeAdminRole(userIdentity, roleName);

    userAuditService.saveUserAudit(
        userIdentity, roleName, AuditType.ADMIN_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(response);
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

    // Validate the arguments
    if (Strings.isBlank(performedBy) && Strings.isBlank(performedOn)) {
      throw new CTPException(
          Fault.BAD_REQUEST,
          "Nothing to search for. Either 'performedBy' or 'performedOn' must be supplied");
    } else if (Strings.isNotBlank(performedBy) && Strings.isNotBlank(performedOn)) {
      throw new CTPException(
          Fault.BAD_REQUEST, "Only one of 'performedBy' or 'performedOn' must be supplied");
    }

    // Search the audit table
    List<UserAuditDTO> auditHistory = userAuditService.searchAuditHistory(performedBy, performedOn);

    return ResponseEntity.ok(auditHistory);
  }
}

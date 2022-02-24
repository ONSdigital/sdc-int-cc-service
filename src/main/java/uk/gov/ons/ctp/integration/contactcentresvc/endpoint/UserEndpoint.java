package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RoleService;
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
  private RoleService roleService;

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
      final UserAuditService userAuditService,
      final RoleService roleService) {
    this.rbacService = rbacService;
    this.userService = userService;
    this.userAuditService = userAuditService;
    this.roleService = roleService;
  }

  @GetMapping("/{userName}")
  public ResponseEntity<UserDTO> getUserByName(
      @PathVariable(value = "userName") @Valid @Email String userName) throws CTPException {

    log.info("Entering getUserByName", kv("userName", userName));
    rbacService.assertUserPermission(PermissionType.READ_USER);

    return ResponseEntity.ok(userService.getUser(userName));
  }

  @GetMapping("/permissions")
  public ResponseEntity<Set<PermissionType>> getLoggedInUsersPermissions() throws CTPException {

    String userName = UserIdentityContext.get();
    log.info("Entering getLoggedInUsersPermissions", kv("userName", userName));

    rbacService.assertUserValidAndActive();

    // All users need to access this so no permission assertion
    List<RoleDTO> usersRoles = userService.getUsersRoles(userName);
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

    return ResponseEntity.ok(userService.getUsers());
  }

  @PutMapping("/{userName}")
  public ResponseEntity<UserDTO> modifyUser(
      @PathVariable(value = "userName") @Valid @Email String userName, @RequestBody UserDTO userDTO)
      throws CTPException {

    log.info("Entering modifyUser", kv("userName", userName));
    rbacService.assertUserPermission(PermissionType.MODIFY_USER);
    rbacService.assertNotSelfModification(userName);

    String status = userDTO.isActive() ? "ACTIVE" : "INACTIVE";

    userDTO.setName(userName);

    userAuditService.saveUserAudit(userName, null, AuditType.USER, AuditSubType.MODIFIED, status);

    return ResponseEntity.ok(userService.modifyUser(userDTO));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws CTPException {
    log.info("Entering createUser", kv("userName", userDTO.getName()));

    rbacService.assertUserPermission(PermissionType.CREATE_USER);

    UserDTO createdUser = userService.createUser(userDTO);

    userAuditService.saveUserAudit(
        createdUser.getName(), null, AuditType.USER, AuditSubType.CREATED, null);

    return ResponseEntity.ok(createdUser);
  }

  @PatchMapping("/{userName}/addSurvey/{surveyType}")
  public ResponseEntity<UserDTO> addUserSurvey(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "surveyType") SurveyType surveyType)
      throws CTPException {
    log.info("Entering addUserSurvey", kv("userName", userName), kv("surveyType", surveyType));

    rbacService.assertUserPermission(PermissionType.USER_SURVEY_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, null, AuditType.USER_SURVEY_USAGE, AuditSubType.ADDED, surveyType.name());

    return ResponseEntity.ok(userService.addUserSurvey(userName, surveyType));
  }

  @PatchMapping("/{userName}/removeSurvey/{surveyType}")
  public ResponseEntity<UserDTO> removeUserSurvey(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "surveyType") SurveyType surveyType)
      throws CTPException {
    log.info("Entering removeUserSurvey", kv("userName", userName), kv("surveyType", surveyType));

    rbacService.assertUserPermission(PermissionType.USER_SURVEY_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, null, AuditType.USER_SURVEY_USAGE, AuditSubType.REMOVED, surveyType.name());

    return ResponseEntity.ok(userService.removeUserSurvey(userName, surveyType));
  }

  @PatchMapping("/{userName}/addUserRole/{roleName}")
  public ResponseEntity<UserDTO> addUserRole(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addUserRole", kv("userName", userName), kv("roleName", roleName));
    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, roleName, AuditType.USER_ROLE, AuditSubType.ADDED, null);

    return ResponseEntity.ok(userService.addUserRole(userName, roleName));
  }

  @PatchMapping("/{userName}/removeUserRole/{roleName}")
  public ResponseEntity<UserDTO> removeUserRole(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {
    log.info("Entering removeUserRole", kv("userName", userName), kv("roleName", roleName));

    rbacService.assertAdminPermission(roleName, PermissionType.USER_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, roleName, AuditType.USER_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.removeUserRole(userName, roleName));
  }

  @PatchMapping("/{userName}/addAdminRole/{roleName}")
  public ResponseEntity<UserDTO> addAdminRole(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering addAdminRole", kv("userName", userName), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, roleName, AuditType.ADMIN_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.addAdminRole(userName, roleName));
  }

  @PatchMapping("/{userName}/removeAdminRole/{roleName}")
  public ResponseEntity<UserDTO> removeAdminRole(
      @PathVariable(value = "userName") @Valid @Email String userName,
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering removeAdminRole", kv("userName", userName), kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.ADMIN_ROLE_MAINTENANCE);
    rbacService.assertNotSelfModification(userName);

    userAuditService.saveUserAudit(
        userName, roleName, AuditType.ADMIN_ROLE, AuditSubType.REMOVED, null);

    return ResponseEntity.ok(userService.removeAdminRole(userName, roleName));
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CreateRoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RoleService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.UserAuditService;

@Slf4j
@RestController
@RequestMapping(value = "/roles", produces = "application/json")
public class RoleEndpoint {
  private RoleService roleService;
  private RBACService rbacService;
  private UserAuditService userAuditService;

  @Autowired
  public RoleEndpoint(
      final RoleService roleService,
      final RBACService rbacService,
      final UserAuditService userAuditService) {
    this.roleService = roleService;
    this.rbacService = rbacService;
    this.userAuditService = userAuditService;
  }

  @GetMapping
  public ResponseEntity<List<RoleDTO>> getRoles() throws CTPException {

    log.info("Entering getRoles");
    rbacService.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.getRoles());
  }

  @GetMapping("/{roleName}")
  public ResponseEntity<RoleDTO> getRoleByName(@PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering getRoleByName", kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.getRole(roleName));
  }

  @GetMapping("/{roleName}/users")
  public ResponseEntity<List<String>> findUsersForUserRole(
      @PathVariable(value = "roleName") String roleName) throws CTPException {

    log.info("Entering findUsersForUserRole", kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.findUsersForUserRole(roleName));
  }

  @GetMapping("/{roleName}/admins")
  public ResponseEntity<List<String>> findUsersForAdminRole(
      @PathVariable(value = "roleName") String roleName) throws CTPException {

    log.info("Entering findUsersForAdminRole", kv("roleName", roleName));
    rbacService.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.findUsersForAdminRole(roleName));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<CreateRoleDTO> createRole(@RequestBody CreateRoleDTO createRoleDTO) throws CTPException {

    log.info("Entering createRole", kv("roleName", createRoleDTO.getName()));
    rbacService.assertUserPermission(PermissionType.CREATE_ROLE);

    CreateRoleDTO createdRole = roleService.createRole(createRoleDTO);

    userAuditService.saveUserAudit(
        null, createdRole.getName(), AuditType.ROLE, AuditSubType.CREATED, null);

    return ResponseEntity.ok(createdRole);
  }

  @PatchMapping("/{roleName}/addPermission/{permissionType}")
  public ResponseEntity<RoleDTO> addPermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    log.info("Entering addPermission", kv("permissionType", permissionType));
    rbacService.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);

    userAuditService.saveUserAudit(
        null, roleName, AuditType.PERMISSION, AuditSubType.ADDED, permissionType.name());

    return ResponseEntity.ok(roleService.addPermission(roleName, permissionType));
  }

  @PatchMapping("/{roleName}/removePermission/{permissionType}")
  public ResponseEntity<RoleDTO> removePermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    log.info("Entering removePermission", kv("permissionType", permissionType));
    rbacService.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);

    userAuditService.saveUserAudit(
        null, roleName, AuditType.PERMISSION, AuditSubType.REMOVED, permissionType.name());

    return ResponseEntity.ok(roleService.removePermission(roleName, permissionType));
  }
}

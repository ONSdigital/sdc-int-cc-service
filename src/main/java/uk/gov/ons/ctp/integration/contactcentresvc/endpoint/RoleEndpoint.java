package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RoleService;

@Slf4j
@RestController
@RequestMapping(value = "/roles", produces = "application/json")
public class RoleEndpoint {
  private RoleService roleService;
  private UserIdentityHelper identityHelper;

  @Autowired
  public RoleEndpoint(final RoleService roleService, final UserIdentityHelper identityHelper) {
    this.roleService = roleService;
    this.identityHelper = identityHelper;
  }

  @GetMapping
  public ResponseEntity<List<RoleDTO>> getRoles() throws CTPException {

    log.info("Entering getRoles");
    identityHelper.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.getRoles());
  }

  @GetMapping("/{roleName}")
  public ResponseEntity<RoleDTO> getRoleByName(@PathVariable(value = "roleName") String roleName)
      throws CTPException {

    log.info("Entering getRoleByName", kv("roleName", roleName));
    identityHelper.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.getRole(roleName));
  }

  @GetMapping("/{roleName}/users")
  public ResponseEntity<List<String>> findUsersForUserRole(
      @PathVariable(value = "roleName") String roleName) throws CTPException {

    log.info("Entering findUsersForUserRole", kv("roleName", roleName));
    identityHelper.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.findUsersForUserRole(roleName));
  }

  @GetMapping("/{roleName}/admins")
  public ResponseEntity<List<String>> findUsersForAdminRole(
      @PathVariable(value = "roleName") String roleName) throws CTPException {

    log.info("Entering findUsersForAdminRole", kv("roleName", roleName));
    identityHelper.assertUserPermission(PermissionType.READ_ROLE);
    return ResponseEntity.ok(roleService.findUsersForAdminRole(roleName));
  }

  @PostMapping
  public ResponseEntity<RoleDTO> createRole(@RequestBody RoleDTO roleDTO) throws CTPException {

    log.info("Entering createRole", kv("roleName", roleDTO.getName()));
    identityHelper.assertUserPermission(PermissionType.CREATE_ROLE);
    return ResponseEntity.ok(roleService.createRole(roleDTO));
  }

  @PatchMapping("/{roleName}/addPermission/{permissionType}")
  public ResponseEntity<RoleDTO> addPermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    log.info("Entering addPermission", kv("permissionType", permissionType));
    identityHelper.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);
    return ResponseEntity.ok(roleService.addPermission(roleName, permissionType));
  }

  @PatchMapping("/{roleName}/removePermission/{permissionType}")
  public ResponseEntity<RoleDTO> removePermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    log.info("Entering removePermission", kv("permissionType", permissionType));
    identityHelper.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);
    return ResponseEntity.ok(roleService.removePermission(roleName, permissionType));
  }
}

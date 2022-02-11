package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@Slf4j
// @Timed
@RestController
@RequestMapping(value = "/roles", produces = "application/json")
public class RoleEndpoint {

  private MapperFacade mapper;

  private RoleRepository roleRepository;

  private PermissionRepository permissionRepository;

  private UserIdentityHelper identityHelper;

  @Autowired
  public RoleEndpoint(
      final RoleRepository roleRepository,
      final PermissionRepository permissionRepository,
      final MapperFacade mapper,
      final UserIdentityHelper identityHelper) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.mapper = mapper;
    this.identityHelper = identityHelper;
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<RoleDTO>> getRoles() throws CTPException {

    identityHelper.assertUserPermission(PermissionType.READ_ROLE);

    List<RoleDTO> dtoList = mapper.mapAsList(roleRepository.findAll(), RoleDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/{roleName}")
  @Transactional
  public ResponseEntity<RoleDTO> getRoleByName(
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.READ_ROLE);

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @GetMapping("/{roleName}/users")
  @Transactional
  public ResponseEntity<List<String>> findUsersForUserRole(
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.READ_ROLE);

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return ResponseEntity.ok(role.getUsers().stream().filter(u->u.isActive()).map(u->u.getName()).collect(Collectors.toList()));
  }

  @GetMapping("/{roleName}/admins")
  @Transactional
  public ResponseEntity<List<String>> findUsersForAdminRole(
      @PathVariable(value = "roleName") String roleName)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.READ_ROLE);

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return ResponseEntity.ok(role.getUsers().stream().filter(u->u.isActive()).map(u->u.getName()).collect(Collectors.toList()));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<RoleDTO> createRole(@RequestBody RoleDTO roleDTO)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.CREATE_ROLE);

    if (roleRepository.findByName(roleDTO.getName()).isPresent()) {
      throw new CTPException(Fault.BAD_REQUEST, "Role with that name already exists");
    }
    Role role = new Role();
    role.setId(UUID.randomUUID());
    role.setName(roleDTO.getName());

    roleRepository.saveAndFlush(role);
    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @PatchMapping("/{roleName}/addPermission/{permissionType}")
  @Transactional
  public ResponseEntity<RoleDTO> addPermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Optional<Permission> permissionOpt =
        permissionRepository.findByPermissionTypeAndRole(permissionType, role);
    if (permissionOpt.isEmpty()) {
      Permission permission = new Permission();
      permission.setId(UUID.randomUUID());
      permission.setRole(role);
      permission.setPermissionType(permissionType);
      role.getPermissions().add(permission);
      roleRepository.saveAndFlush(role);
    }

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @PatchMapping("/{roleName}/removePermission/{permissionType}")
  @Transactional
  public ResponseEntity<RoleDTO> removePermission(
      @PathVariable(value = "roleName") String roleName,
      @PathVariable(value = "permissionType") PermissionType permissionType)
      throws CTPException {

    identityHelper.assertUserPermission(PermissionType.MAINTAIN_PERMISSIONS);

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Optional<Permission> permissionOpt =
        permissionRepository.findByPermissionTypeAndRole(permissionType, role);
    if (permissionOpt.isEmpty()) {
      Permission permission = new Permission();
      log.info("Removing permission " + permission);
      permissionRepository.delete(permission);
      role.getPermissions().remove(permission);
      roleRepository.saveAndFlush(role);
    }

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }
}

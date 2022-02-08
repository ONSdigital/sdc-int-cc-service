package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.BasicUserDTO;
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
  public ResponseEntity<List<RoleDTO>> getRoles(
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    List<RoleDTO> dtoList = mapper.mapAsList(roleRepository.findAll(), RoleDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/{roleName}")
  @Transactional
  public ResponseEntity<RoleDTO> getRoleByName(
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @Transactional
  @GetMapping("/{roleName}/users")
  public ResponseEntity<List<BasicUserDTO>> findUsersForUserRole(
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    List<BasicUserDTO> dtoList = mapper.mapAsList(role.getUsers(), BasicUserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @Transactional
  @GetMapping("/{roleName}/admins")
  public ResponseEntity<List<BasicUserDTO>> findUsersForAdminRole(
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    List<BasicUserDTO> dtoList = mapper.mapAsList(role.getAdmins(), BasicUserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<RoleDTO> createRole(
      @RequestBody RoleDTO roleDTO,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    if (roleRepository
        .findByName(roleDTO.getName()).isPresent()) {
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
      @PathVariable(value = "permissionType") PermissionType permissionType,
      @RequestAttribute(value = "principal") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository.findByName(roleName).orElseThrow(
        () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Optional<Permission> permissionOpt = permissionRepository.findByPermissionTypeAndRole(permissionType, role);
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
      @PathVariable(value = "permissionType") PermissionType permissionType,
      @RequestAttribute(value = "principal") String principal) throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository.findByName(roleName).orElseThrow(
        () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));


    Optional<Permission> permissionOpt = permissionRepository.findByPermissionTypeAndRole(permissionType, role);
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

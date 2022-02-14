package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;

@Slf4j
@Service
public class RoleService {
  @Autowired MapperFacade mapper;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private RoleRepository roleRepository;

  @Transactional
  public RoleDTO getRole(String roleName) throws CTPException {

    log.info("Entering getRole", kv("roleName", roleName));
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return mapper.map(role, RoleDTO.class);
  }

  @Transactional
  public List<RoleDTO> getRoles() throws CTPException {

    log.info("Entering getRoles");
    List<RoleDTO> dtoList = mapper.mapAsList(roleRepository.findAll(), RoleDTO.class);
    return dtoList;
  }

  @Transactional
  public List<String> findUsersForUserRole(String roleName) throws CTPException {

    log.info("Entering findUsersForUserRole", kv("roleName", roleName));
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return role.getUsers().stream()
        .filter(u -> u.isActive())
        .map(u -> u.getName())
        .collect(Collectors.toList());
  }

  @Transactional
  public List<String> findUsersForAdminRole(String roleName) throws CTPException {

    log.info("Entering findUsersForAdminRole", kv("roleName", roleName));
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return role.getUsers().stream()
        .filter(u -> u.isActive())
        .map(u -> u.getName())
        .collect(Collectors.toList());
  }

  @Transactional
  public RoleDTO createRole(RoleDTO roleDTO) throws CTPException {

    log.info("Entering createRole", kv("roleName", roleDTO.getName()));
    if (roleRepository.findByName(roleDTO.getName()).isPresent()) {
      throw new CTPException(Fault.BAD_REQUEST, "Role with that name already exists");
    }
    Role role = new Role();
    role.setId(UUID.randomUUID());
    role.setName(roleDTO.getName());

    roleRepository.saveAndFlush(role);
    return mapper.map(role, RoleDTO.class);
  }

  @Transactional
  public RoleDTO addPermission(String roleName, PermissionType permissionType) throws CTPException {

    log.info("Entering addPermission", kv("permissionType", permissionType));
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

    return mapper.map(role, RoleDTO.class);
  }

  @Transactional
  public RoleDTO removePermission(String roleName, PermissionType permissionType)
      throws CTPException {

    log.info("Entering removePermission", kv("permissionType", permissionType));
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Optional<Permission> permissionOpt =
        permissionRepository.findByPermissionTypeAndRole(permissionType, role);
    if (permissionOpt.isEmpty()) {
      Permission permission = new Permission();
      permissionRepository.delete(permission);
      role.getPermissions().remove(permission);
      roleRepository.saveAndFlush(role);
    }

    return mapper.map(role, RoleDTO.class);
  }
}

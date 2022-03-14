package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

/**
 * Separate class that can create/update database items and commit the results
 * so that subsequent operations can see the effect.
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalOps {
  private UserRepository userRepo;
  private UserAuditRepository userAuditRepository;
  private RoleRepository roleRepository;

  public TransactionalOps(UserRepository userRepo, UserAuditRepository userAuditRepository,
      RoleRepository roleRepository) {
    this.userRepo = userRepo;
    this.userAuditRepository = userAuditRepository;
    this.roleRepository = roleRepository;
  }

  public void deleteAll() {
    userAuditRepository.deleteAll();
    userRepo.deleteAll();
    roleRepository.deleteAll();
  }

  public void createUser(String name, UUID id) {
    createUser(name, id, null, null);
  }

  public void createUser(String name, UUID id, List<Role> userRoles, List<Role> adminRoles) {
    User user = User.builder().id(id).identity(name)
        .userRoles(userRoles == null ? Collections.emptyList() : userRoles)
        .adminRoles(adminRoles == null ? Collections.emptyList() : adminRoles).build();
    userRepo.save(user);
  }

  public Role createRole(String name, UUID id, List<PermissionType> permTypes) {
    permTypes = permTypes == null ? new ArrayList<>() : permTypes;
    Role role = Role.builder().id(id).name(name).permissions(new ArrayList<>()).build();

    permTypes.stream().forEach(type -> {
      addPermission(role, type);
    });
    return roleRepository.save(role);
  }

  public Role addPermission(Role role, PermissionType type) {
    Permission p = Permission.builder().id(UUID.randomUUID()).permissionType(type).role(role).build();
    role.getPermissions().add(p);
    roleRepository.save(role);
    return role;
  }
}

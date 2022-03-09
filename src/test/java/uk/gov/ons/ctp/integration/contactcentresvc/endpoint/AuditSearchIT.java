package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.RoleEndpointCaller;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.UserEndpointCaller;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;

/** Integration test for user login. */
public class AuditSearchIT extends FullStackIntegrationTestBase {

  private static UUID SUPERUSER_UUID = UUID.fromString("SU");
  private static final UUID CREATOR_ROLE_ID = UUID.fromString("acaffd8b-66a2-4948-a0cc-f6536c8a9fe0");

  @Autowired
  private TransactionalOps txOps;

  URL base;
  @LocalServerPort
  int port;

  RoleEndpointCaller roleEndpoint;
  UserEndpointCaller userEndpoint;

  @BeforeEach
  public void setup() throws MalformedURLException {
    base = new URL("http://localhost:" + port);

    userEndpoint = new UserEndpointCaller(base);
    roleEndpoint = new RoleEndpointCaller(base);

    txOps.deleteAll();

    // Bootstrap an initial user
    List<PermissionType> permissions = List.of(PermissionType.CREATE_USER, 
        PermissionType.CREATE_ROLE, 
        PermissionType.MAINTAIN_PERMISSIONS,
        PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE,
        PermissionType.RESERVED_USER_ROLE_ADMIN);
    Role role = txOps.createRole("creator", CREATOR_ROLE_ID, permissions);
    txOps.createUser("superuser@ext.ons.gov.uk", SUPERUSER_UUID, List.of(role), null);
  }

  @Test
  public void login() throws Exception {
    
    // Build some audit history:
    SU | superuser@ext.ons.gov.uk | t      | [null]   | [null]  | f
    TL | BossMan@ext.ons.gov.uk   | t      | John     | Smith   | f
SU

RoleA = RoleAdmin


    // SU TL null USER CREATED --
    // SU -- RoleA ROLE CREATED -- 
    // SU TL RoleA ADMIN_ROLE ADDED --
    // SU -- Admin ROLE CREATED --
    // SU -- Admin PERMISSION ADDED READ_USER_AUDIT
    // SU TL Admin USER_ROLE ADDED --
    // SU TL TL -- LOGIN -- --
LOGOUT ??? no-audit
        
    // Create some users
    userEndpoint.invokeCreateUser("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk");
    
    // Create group for role administration
    roleEndpoint.invokeCreateRole("superuser@ext.ons.gov.uk", "RoleAdmin", "", PermissionType.USER_ROLE_MAINTENANCE);
    userEndpoint.addAdminRole("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", "RoleAdmin");

    // Allow BossMan to use the audit endpoint
    roleEndpoint.invokeCreateRole("superuser@ext.ons.gov.uk", "Admin", "For administrators only");
    roleEndpoint.invokeAddPermission("superuser@ext.ons.gov.uk", "Admin", PermissionType.READ_USER_AUDIT);
    userEndpoint.addUserRole("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", "Admin");

    // Create some user activity
    userEndpoint.invokeLogin("BossMan@ext.ons.gov.uk");
    userEndpoint.invokeLogout("BossMan@ext.ons.gov.uk");

    List<UserAuditDTO> audit = userEndpoint.invokeAudit("BossMan@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", null).getBody();
    assertEquals(1, audit.size());
  }

  /**
   * Separate class that can create/update database items and commit the results
   * so that subsequent operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
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
      userRepo.deleteAll();
      userAuditRepository.deleteAll();
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
}

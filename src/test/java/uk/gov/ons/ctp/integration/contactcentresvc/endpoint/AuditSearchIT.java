package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  private static UUID SUPERUSER_UUID = UUID.fromString("f26d0bd8-77ce-4eeb-860f-6ab52c355b7d");
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
    super.init();
    
    base = new URL("http://localhost:" + port);

    userEndpoint = new UserEndpointCaller(base);
    roleEndpoint = new RoleEndpointCaller(base);

    txOps.deleteAll();

    // Bootstrap an initial user
    List<PermissionType> permissions = List.of(
        PermissionType.CREATE_USER, 
        PermissionType.CREATE_ROLE, 
        PermissionType.MAINTAIN_PERMISSIONS,
        PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE,
        PermissionType.RESERVED_USER_ROLE_ADMIN);
    Role role = txOps.createRole("creator", CREATOR_ROLE_ID, permissions);
    txOps.createUser("superuser@ext.ons.gov.uk", SUPERUSER_UUID, List.of(role), null);
  }

  @Test
  public void auditSearch() throws Exception {
    
    // Build some audit history:
//SU
//
//RoleA = RoleAdmin

    // SU -- Admin ROLE CREATED --
    // SU -- Admin PERMISSION ADDED READ_USER_AUDIT
    // SU TL null USER CREATED --
    // SU TL Admin USER_ROLE ADDED --
    // TL TL -- LOGIN -- --
    // TL TL -- LOGOUT -- -- 

    long start = System.currentTimeMillis();
    
    // Create group that allows invocation of audit endpoint
    roleEndpoint.invokeCreateRole("superuser@ext.ons.gov.uk", "Admin", "For administrators only");
    roleEndpoint().invokeAddPermission("superuser@ext.ons.gov.uk", "Admin", PermissionType.READ_USER_AUDIT);
    
    // Create a team leader and allow them to use the audit endpoint
    userEndpoint().invokeCreateUser("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk");
    userEndpoint.addUserRole("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", "Admin");

    // Create some user activity
    userEndpoint.invokeLogin("BossMan@ext.ons.gov.uk");
    userEndpoint.invokeLogout("BossMan@ext.ons.gov.uk");

    List<UserAuditDTO> audit = userEndpoint.invokeAudit("BossMan@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", null).getBody();
    assertEquals(2, audit.size());

    List<UserAuditDTO> audit2 = userEndpoint.invokeAudit("BossMan@ext.ons.gov.uk", null, "BossMan@ext.ons.gov.uk").getBody();
    assertEquals(4, audit2.size());

    List<UserAuditDTO> audit3 = userEndpoint.invokeAudit("BossMan@ext.ons.gov.uk", "superuser@ext.ons.gov.uk", null).getBody();
    assertEquals(4, audit3.size());

    List<UserAuditDTO> audit4 = userEndpoint.invokeAudit("BossMan@ext.ons.gov.uk", null, "superuser@ext.ons.gov.uk").getBody();
    assertEquals(0, audit4.size());

    long end = System.currentTimeMillis();
    long runTime = end - start;
    System.out.println("Runtime: " + runTime);
    assertTrue(runTime <1000);
  }

  @Test
  public void noPermissionToAudit() throws Exception {
    
    long start = System.currentTimeMillis();
    
    // Create a team leader and allow them to use the audit endpoint
    userEndpoint.invokeCreateUser("superuser@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk");

    ResponseEntity<String> audit = userEndpoint.invokeAudit(HttpStatus.UNAUTHORIZED, "BossMan@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", null);

    long end = System.currentTimeMillis();
    long runTime = end - start;
    System.out.println("Not auth runtime: " + runTime);
    assertTrue(runTime <100000);

    assertTrue(audit.getBody().contains("User not authorised for activity READ_USER_AUDIT"), audit.getBody());
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
}

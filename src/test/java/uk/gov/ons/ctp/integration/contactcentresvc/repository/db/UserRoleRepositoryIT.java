package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;

/** Database tests for users, roles and permissions. */
public class UserRoleRepositoryIT extends PostgresTestBase {
  private static final UUID FRED_UUID = UUID.fromString("5d788f49-a256-4ae3-9fcf-5d59e8ad4228");
  private static final UUID JOE_UUID = UUID.fromString("ff838710-4b6f-11ec-b4ab-4c3275913db5");
  private static final UUID SHOPKEEPER_UUID =
      UUID.fromString("2733fc72-4b70-11ec-adec-4c3275913db5");
  private static final UUID NURSE_UUID = UUID.fromString("1e0fd448-4c3a-11ec-924e-4c3275913db5");

  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private PermissionRepository permRepo;
  @Autowired private TransactionalOps txOps;

  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }

  @Test
  public void shouldBeCleanBeforeEachTest() {
    assertTrue(userRepo.findAll().isEmpty());
    assertTrue(roleRepo.findAll().isEmpty());
    assertTrue(permRepo.findAll().isEmpty());
  }

  @Test
  public void shouldFindUser() {
    txOps.createUser("Fred", FRED_UUID);

    Optional<User> fredOpt = userRepo.findByName("Fred");
    assert(fredOpt.isPresent());
    User fred = fredOpt.get();
    assertEquals("Fred", fred.getName());
    assertEquals(FRED_UUID, fred.getId());
    assertTrue(fred.isActive());
  }

  @Test
  public void shouldCreateUser() {
    txOps.createUser("Joe", JOE_UUID);
    assertEquals("Joe", userRepo.findByName("Joe").get().getName());
    assertEquals("Joe", userRepo.getById(JOE_UUID).getName());
  }

  @Test
  public void shouldDeleteUser() {
    txOps.createUser("Joe", JOE_UUID);
    Optional<User> joe = userRepo.findByName("Joe");
    assert(joe.isPresent());
    userRepo.delete(joe.get());
    assert(userRepo.findByName("Joe").isEmpty());
  }

  @Test
  public void shouldCreateRole() {
    txOps.createRole("nurse", NURSE_UUID, null);
    Role nurse = roleRepo.findByName("nurse");
    assertEquals("nurse", nurse.getName());
    assertTrue(nurse.getPermissions().isEmpty());
  }

  @Test
  public void shouldCreateRoleWithPermissions() {
    var perms =
        Arrays.asList(
            new PermissionType[] {PermissionType.SEARCH_CASES, PermissionType.VIEW_CASE_DETAILS});
    txOps.createRole("nurse", NURSE_UUID, perms);
    Role nurse = roleRepo.findByName("nurse");
    assertEquals("nurse", nurse.getName());
    assertEquals(2, nurse.getPermissions().size());
    assertEquals(2, permRepo.findAll().size());
  }

  @Test
  public void shouldAddPermission() {
    Role role = txOps.createRole("nurse", NURSE_UUID, null);
    txOps.addPermission(role, PermissionType.SUPER_USER);
    Role nurse = roleRepo.findByName("nurse");
    assertEquals(1, nurse.getPermissions().size());
    assertEquals(1, permRepo.findAll().size());
    assertEquals(PermissionType.SUPER_USER, nurse.getPermissions().get(0).getPermissionType());
  }

  @Test
  public void shouldRemovePermission() {
    // create role with 2 permissions
    var perms =
        Arrays.asList(
            new PermissionType[] {PermissionType.SEARCH_CASES, PermissionType.VIEW_CASE_DETAILS});
    Role role = txOps.createRole("nurse", NURSE_UUID, perms);
    // now remove one of those permissions
    txOps.removePermission(role, PermissionType.VIEW_CASE_DETAILS);
    Role nurse = roleRepo.findByName("nurse");
    assertEquals(1, nurse.getPermissions().size());
    assertEquals(1, permRepo.findAll().size());
    assertEquals(PermissionType.SEARCH_CASES, nurse.getPermissions().get(0).getPermissionType());
  }

  @Test
  public void shouldAssignRoleToUser() {
    txOps.createJoeTheShopkeeper();
    txOps.verifyJoeTheShopkeeper();
  }

  @Test
  public void shouldAssignAdminRoleToUser() {
    txOps.createJoeTheShopkeeperAdmin();
    txOps.verifyJoeTheShopkeeperAdmin();
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;
    private RoleRepository roleRepo;
    private PermissionRepository permRepo;

    public TransactionalOps(
        UserRepository userRepo, RoleRepository roleRepo, PermissionRepository permRepo) {
      this.userRepo = userRepo;
      this.roleRepo = roleRepo;
      this.permRepo = permRepo;
    }

    public void deleteAll() {
      userRepo.deleteAll();
      roleRepo.deleteAll();
      permRepo.deleteAll();
    }

    public void createUser(String name, UUID id) {
      createUser(name, id, null, null);
    }

    public Role addPermission(Role role, PermissionType type) {
      Permission p =
          Permission.builder().id(UUID.randomUUID()).permissionType(type).role(role).build();
      role.getPermissions().add(p);
      roleRepo.save(role);
      return role;
    }

    public void removePermission(Role role, PermissionType type) {
      Permission p = permRepo.findByPermissionTypeAndRole(type, role);
      permRepo.delete(p);
    }

    public Role createRole(String name, UUID id, List<PermissionType> permTypes) {
      permTypes = permTypes == null ? new ArrayList<>() : permTypes;
      Role role = Role.builder().id(id).name(name).permissions(new ArrayList<>()).build();

      permTypes.stream()
          .forEach(
              type -> {
                addPermission(role, type);
              });
      return roleRepo.save(role);
    }

    public void createUser(String name, UUID id, List<Role> userRoles, List<Role> adminRoles) {
      User user =
          User.builder()
              .id(id)
              .name(name)
              .userRoles(userRoles == null ? Collections.emptyList() : userRoles)
              .adminRoles(adminRoles == null ? Collections.emptyList() : adminRoles)
              .build();
      userRepo.save(user);
    }

    public void createJoeTheShopkeeper() {
      var role = createRole("shopkeeper", SHOPKEEPER_UUID, null);
      var roles = new ArrayList<Role>();
      roles.add(role);
      createUser("Joe", JOE_UUID, roles, null);
    }

    public void verifyJoeTheShopkeeper() {
      Optional<User> joe = userRepo.findByName("Joe");
      Role shopkeeper = roleRepo.findByName("shopkeeper");
      assertEquals(shopkeeper, joe.get().getUserRoles().get(0));
      assertEquals(joe.get(), shopkeeper.getUsers().get(0));
    }

    public void createJoeTheShopkeeperAdmin() {
      var role = createRole("shopkeeper", SHOPKEEPER_UUID, null);
      var roles = new ArrayList<Role>();
      roles.add(role);
      createUser("Joe", JOE_UUID, null, roles);
    }

    public void verifyJoeTheShopkeeperAdmin() {
      Optional<User> joe = userRepo.findByName("Joe");
      Role shopkeeper = roleRepo.findByName("shopkeeper");
      assertEquals(shopkeeper, joe.get().getAdminRoles().get(0));
      assertEquals(joe.get(), shopkeeper.getAdmins().get(0));
    }
  }
}

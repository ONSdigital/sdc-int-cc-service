package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    Optional<User> fredOpt = userRepo.findByIdentity("Fred");
    assert (fredOpt.isPresent());
    User fred = fredOpt.get();
    assertEquals("Fred", fred.getIdentity());
    assertEquals(FRED_UUID, fred.getId());
    assertTrue(fred.isActive());
  }

  @Test
  public void shouldCreateUser() {
    txOps.createUser("Joe", JOE_UUID);
    assertEquals("Joe", userRepo.findByIdentity("Joe").get().getIdentity());
    assertEquals("Joe", userRepo.getById(JOE_UUID).getIdentity());
  }

  @Test
  public void shouldDeleteUser() {
    txOps.createUser("Joe", JOE_UUID);
    Optional<User> joe = userRepo.findByIdentity("Joe");
    assert (joe.isPresent());
    userRepo.delete(joe.get());
    assert (userRepo.findByIdentity("Joe").isEmpty());
  }

  @Test
  public void shouldCreateRole() {
    txOps.createRole("nurse", NURSE_UUID, null);
    Optional<Role> nurse = roleRepo.findByName("nurse");
    assertEquals("nurse", nurse.get().getName());
    assertTrue(nurse.get().getPermissions().isEmpty());
  }

  @Test
  public void shouldCreateRoleWithPermissions() {
    var perms =
        Arrays.asList(new PermissionType[] {PermissionType.SEARCH_CASES, PermissionType.VIEW_CASE});
    txOps.createRole("nurse", NURSE_UUID, perms);
    Optional<Role> nurse = roleRepo.findByName("nurse");
    assertEquals("nurse", nurse.get().getName());
    assertEquals(2, nurse.get().getPermissions().size());
    assertEquals(2, permRepo.findAll().size());
  }

  @Test
  public void shouldAddPermission() {
    Role role = txOps.createRole("nurse", NURSE_UUID, null);
    txOps.addPermission(role, PermissionType.READ_USER);
    Optional<Role> nurse = roleRepo.findByName("nurse");
    assertEquals(1, nurse.get().getPermissions().size());
    assertEquals(1, permRepo.findAll().size());
    assertEquals(PermissionType.READ_USER, nurse.get().getPermissions().get(0).getPermissionType());
  }

  @Test
  public void shouldRemovePermission() {
    // create role with 2 permissions
    var perms =
        Arrays.asList(new PermissionType[] {PermissionType.SEARCH_CASES, PermissionType.VIEW_CASE});
    Role role = txOps.createRole("nurse", NURSE_UUID, perms);
    // now remove one of those permissions
    txOps.removePermission(role, PermissionType.VIEW_CASE);
    Optional<Role> nurse = roleRepo.findByName("nurse");
    assertEquals(1, nurse.get().getPermissions().size());
    assertEquals(1, permRepo.findAll().size());
    assertEquals(
        PermissionType.SEARCH_CASES, nurse.get().getPermissions().get(0).getPermissionType());
  }

  @Test
  public void shouldAssignRoleToUser() {
    Role role = txOps.createRole("shopkeeper", SHOPKEEPER_UUID, null);
    ArrayList<Role> roles = new ArrayList<>();
    roles.add(role);
    txOps.createUser("Joe", SHOPKEEPER_UUID, roles, null);

    txOps.verifyNormalUserAndRole("Joe", "shopkeeper");
  }

  @Test
  public void shouldAssignAdminRoleToUser() {
    Role role = txOps.createRole("shopkeeper", SHOPKEEPER_UUID, null);
    ArrayList<Role> roles = new ArrayList<>();
    roles.add(role);
    txOps.createUser("Joe", SHOPKEEPER_UUID, null, roles);

    txOps.verifyAdminUserAndRole("Joe", "shopkeeper");
  }
}

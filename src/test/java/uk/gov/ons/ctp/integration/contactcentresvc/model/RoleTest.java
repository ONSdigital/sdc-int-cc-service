package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RoleTest {
  private Role role;
  private User u1;
  private User u2;

  @Test
  public void shouldHaveNoArgConstructor() {
    role = new Role();
    assertNull(role.getName());
  }

  @Test
  public void shouldHaveSetters() {
    role = new Role();
    role.setId(UUID.randomUUID());
    role.setName("coder");
    role.setPermissions(new ArrayList<>());
    role.setUsers(new ArrayList<>());
    role.setAdmins(new ArrayList<>());
    assertNotNull(role.getId());
    assertNotNull(role.getName());
    assertNotNull(role.getPermissions());
    assertNotNull(role.getUsers());
    assertNotNull(role.getAdmins());
  }

  @Test
  public void shouldCreateRole() {
    createRole();

    assertEquals("coder", role.getName());
    assertEquals(2, role.getPermissions().size());
    assertEquals(u1, role.getAdmins().get(0));
    assertEquals(u2, role.getUsers().get(0));
  }

  @Test
  public void shouldHaveSafeToString() {
    createRole();
    String s = role.toString();
    assertTrue(s.contains("coder"));
    assertTrue(s.contains(role.getId().toString()));
    assertFalse(s.contains(u1.getName()));
    assertFalse(s.contains(u2.getName()));
    assertFalse(s.contains(PermissionType.SEARCH_CASES.name()));
    assertFalse(s.contains(PermissionType.VIEW_CASE_DETAILS.name()));
  }

  private void createRole() {
    Permission p1 =
        Permission.builder()
            .id(UUID.randomUUID())
            .permissionType(PermissionType.SEARCH_CASES)
            .build();

    Permission p2 =
        Permission.builder()
            .id(UUID.randomUUID())
            .permissionType(PermissionType.VIEW_CASE_DETAILS)
            .build();

    role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("coder")
            .permissions(new ArrayList<>())
            .admins(new ArrayList<>())
            .users(new ArrayList<>())
            .build();

    role.getPermissions().add(p1);
    role.getPermissions().add(p2);

    u1 = createUser("Joe");
    u2 = createUser("Jasmine");

    role.getAdmins().add(u1);
    role.getUsers().add(u2);
  }

  private User createUser(String name) {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .name(name)
            .active(true)
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .build();
    return user;
  }
}

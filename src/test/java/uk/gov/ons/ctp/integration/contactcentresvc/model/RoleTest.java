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
  private Role r1;
  private Role r2;

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
    role.setUserRoles(new ArrayList<>());
    role.setAdminRoles(new ArrayList<>());
    assertNotNull(role.getId());
    assertNotNull(role.getName());
    assertNotNull(role.getPermissions());
    assertNotNull(role.getUserRoles());
    assertNotNull(role.getAdminRoles());
  }

  @Test
  public void shouldCreateRole() {
    createRole();

    assertEquals("coder", role.getName());
    assertEquals(2, role.getPermissions().size());
    assertEquals(r1, role.getAdminRoles().get(0));
    assertEquals(r2, role.getUserRoles().get(0));
  }

  @Test
  public void shouldHaveSafeToString() {
    createRole();
    String s = role.toString();
    assertTrue(s.contains("coder"));
    assertTrue(s.contains(role.getId().toString()));
    assertFalse(s.contains(r1.getName()));
    assertFalse(s.contains(r2.getName()));
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
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .build();

    role.getPermissions().add(p1);
    role.getPermissions().add(p2);

    r1 = Role.builder().id(UUID.randomUUID()).name("cleaner").build();
    r2 = Role.builder().id(UUID.randomUUID()).name("cook").build();

    role.getAdminRoles().add(r1);
    role.getUserRoles().add(r2);
  }
}

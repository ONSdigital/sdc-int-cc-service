package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class PermissionTest {

  private Permission permission;
  private Role role;

  @Test
  public void shouldHaveNoArgConstructor() {
    permission = new Permission();
    assertNull(permission.getId());
    assertNull(permission.getRole());
  }

  @Test
  public void shouldHaveSetters() {
    permission = new Permission();
    permission.setId(UUID.randomUUID());
    permission.setPermissionType(PermissionType.SEARCH_CASES);
    permission.setRole(new Role());
    assertNotNull(permission.getId());
    assertNotNull(permission.getPermissionType());
    assertNotNull(permission.getRole());
  }

  @Test
  public void shouldCreatePermission() {
    createPermission();
    assertEquals(role, permission.getRole());
    assertEquals(PermissionType.READ_USER, permission.getPermissionType());
  }

  @Test
  public void shouldHaveSafeToString() {
    createPermission();
    String s = permission.toString();
    assertTrue(s.contains(PermissionType.READ_USER.toString()));
    assertTrue(s.contains(permission.getId().toString()));
    assertFalse(s.contains(role.getName()));
  }

  private void createPermission() {
    role = Role.builder().id(UUID.randomUUID()).name("coder").build();

    permission =
        Permission.builder()
            .id(UUID.randomUUID())
            .permissionType(PermissionType.READ_USER)
            .role(role)
            .build();
  }
}

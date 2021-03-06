package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {
  private User user;
  private Role r1;
  private Role r2;

  @Test
  public void shouldHaveNoArgConstructor() {
    user = new User();
    assertNull(user.getIdentity());
    assertTrue(user.isActive());
  }

  @Test
  public void shouldHaveSetters() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setIdentity("Fred");
    user.setActive(false);
    user.setUserRoles(new ArrayList<>());
    user.setAdminRoles(new ArrayList<>());
    assertNotNull(user.getId());
    assertNotNull(user.getIdentity());
    assertNotNull(user.getUserRoles());
    assertNotNull(user.getAdminRoles());
    assertFalse(user.isActive());
  }

  @Test
  public void shouldCreateUser() {
    createUser();

    assertNotNull(user.getId());
    assertEquals("Fred", user.getIdentity());
    assertTrue(user.isActive());
    assertEquals(r1, user.getAdminRoles().get(0));
    assertEquals(r2, user.getUserRoles().get(0));
  }

  @Test
  public void shouldHaveSafeToString() {
    createUser();
    String s = user.toString();
    assertTrue(s.contains("Fred"));
    assertTrue(s.contains(user.getId().toString()));
    assertTrue(s.contains("active"));
    assertFalse(s.contains(r1.getName()));
    assertFalse(s.contains(r2.getName()));
  }

  @Test
  public void shouldHavePermission() {
    createUser();
    assertTrue(user.hasUserPermission(PermissionType.VIEW_CASE));
  }

  @Test
  public void shouldNotHavePermission() {
    createUser();
    assertFalse(user.hasUserPermission(PermissionType.CREATE_USER));
  }

  private void createUser() {
    user =
        User.builder()
            .id(UUID.randomUUID())
            .identity("Fred")
            .active(true)
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .build();

    Permission p =
        Permission.builder().id(UUID.randomUUID()).permissionType(PermissionType.VIEW_CASE).build();
    List<Permission> pList = new ArrayList<Permission>(List.of(p));
    r1 = Role.builder().id(UUID.randomUUID()).name("cleaner").permissions(pList).build();
    r2 = Role.builder().id(UUID.randomUUID()).name("cook").permissions(pList).build();

    user.getAdminRoles().add(r1);
    user.getUserRoles().add(r2);
  }
}

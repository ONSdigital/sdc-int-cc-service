package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {
  private User user;
  private Role r1;
  private Role r2;

  @Test
  public void shouldHaveNoArgConstructor() {
    user = new User();
    assertNull(user.getName());
    assertTrue(user.isActive());
  }

  @Test
  public void shouldHaveSetters() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Fred");
    user.setActive(false);
    user.setUserRoles(new ArrayList<>());
    user.setAdminRoles(new ArrayList<>());
    assertNotNull(user.getId());
    assertNotNull(user.getName());
    assertNotNull(user.getUserRoles());
    assertNotNull(user.getAdminRoles());
    assertFalse(user.isActive());
  }

  @Test
  public void shouldCreateUser() {
    createUser();

    assertNotNull(user.getId());
    assertEquals("Fred", user.getName());
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

  private void createUser() {
    user =
        User.builder()
            .id(UUID.randomUUID())
            .name("Fred")
            .active(true)
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .build();

    r1 = Role.builder().id(UUID.randomUUID()).name("cleaner").build();
    r2 = Role.builder().id(UUID.randomUUID()).name("cook").build();

    user.getAdminRoles().add(r1);
    user.getUserRoles().add(r2);
  }
}

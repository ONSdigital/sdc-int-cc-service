package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

  @Mock RoleRepository roleRepository;

  @Mock PermissionRepository permissionRepository;

  @InjectMocks private RoleService roleService = new RoleService();

  @Test
  public void findUsersForUserRole_deletedUserNotReturned() throws CTPException {
    User user = new User();
    user.setIdentity("User1");
    user.setDeleted(true);

    User user2 = new User();
    user2.setIdentity("User2");

    Role role = new Role();
    role.setUsers(List.of(user, user2));

    when(roleRepository.findByName(any())).thenReturn(Optional.of(role));

    List<String> result = roleService.findUsersForUserRole("role");

    assertEquals(1, result.size());
    assertEquals("User2", result.get(0));
  }

  @Test
  public void findUsersForAdminRole_deletedUserNotReturned() throws CTPException {
    User user = new User();
    user.setIdentity("User1");
    user.setDeleted(true);

    User user2 = new User();
    user2.setIdentity("User2");

    Role role = new Role();
    role.setAdmins(List.of(user, user2));

    when(roleRepository.findByName(any())).thenReturn(Optional.of(role));

    List<String> result = roleService.findUsersForAdminRole("role");

    assertEquals(1, result.size());
    assertEquals("User2", result.get(0));
  }
}

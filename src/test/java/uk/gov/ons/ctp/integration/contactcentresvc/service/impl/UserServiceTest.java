package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Spy private MapperFacade mapper = new CCSvcBeanMapper();
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserSurveyUsageRepository userSurveyUsageRepository;
  @Mock private UserAuditRepository userAuditRepository;

  @InjectMocks UserService userService;

  private final String TEST_USER = "testUser";
  private final String TEST_USER_2 = "testUser2";
  private final String TEST_USER_3 = "testUser3";

  @Test
  public void canBeDeletedTrueWhenZeroCaseInteractionsSpecificUser() throws CTPException {
    User testUser = User.builder().id(UUID.randomUUID()).identity(TEST_USER).build();

    when(userRepository.findByIdentity(TEST_USER)).thenReturn(Optional.of(testUser));

    when(userAuditRepository.existsByCcuserIdAndAuditType(testUser.getId(), AuditType.LOGIN))
        .thenReturn(false);

    UserDTO result = userService.getUser(TEST_USER);

    assertTrue(result.isDeletable());
  }

  @Test
  public void canBeDeletedFalseWhenSomeCaseInteractionsSpecificUser() throws CTPException {
    User testUser = User.builder().id(UUID.randomUUID()).identity(TEST_USER).build();

    when(userRepository.findByIdentity(TEST_USER)).thenReturn(Optional.of(testUser));

    when(userAuditRepository.existsByCcuserIdAndAuditType(testUser.getId(), AuditType.LOGIN))
        .thenReturn(true);

    UserDTO result = userService.getUser(TEST_USER);

    assertFalse(result.isDeletable());
  }

  @Test
  public void canBeDeletedCorrectForMultipleUsers() throws CTPException {

    User test1 = User.builder().id(UUID.randomUUID()).identity(TEST_USER).build();
    User test2 = User.builder().id(UUID.randomUUID()).identity(TEST_USER_2).build();
    User test3 = User.builder().id(UUID.randomUUID()).identity(TEST_USER_3).build();

    List<User> users = List.of(test1, test2, test3);

    when(userRepository.findAll()).thenReturn(users);

    when(userAuditRepository.existsByCcuserIdAndAuditType(test1.getId(), AuditType.LOGIN))
        .thenReturn(false);
    when(userAuditRepository.existsByCcuserIdAndAuditType(test2.getId(), AuditType.LOGIN))
        .thenReturn(true);
    when(userAuditRepository.existsByCcuserIdAndAuditType(test3.getId(), AuditType.LOGIN))
        .thenReturn(false);

    List<UserDTO> results = userService.getUsers();

    assertTrue(results.get(0).isDeletable());
    assertFalse(results.get(1).isDeletable());
    assertTrue(results.get(2).isDeletable());
  }

  @Test
  public void deleteUserWhoIsNotDeletable() throws CTPException {
    User testUser = User.builder().id(UUID.randomUUID()).identity(TEST_USER).build();

    when(userRepository.findByIdentity(TEST_USER)).thenReturn(Optional.of(testUser));

    when(userAuditRepository.existsByCcuserIdAndAuditType(testUser.getId(), AuditType.LOGIN))
        .thenReturn(true);

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userService.deleteUser(TEST_USER);
            });

    assertEquals(CTPException.Fault.BAD_REQUEST, exception.getFault());
    assertEquals("User not deletable", exception.getMessage());
  }
}

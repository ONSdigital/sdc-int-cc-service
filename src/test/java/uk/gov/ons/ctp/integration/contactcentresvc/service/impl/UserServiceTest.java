package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Spy private MapperFacade mapper = new CCSvcBeanMapper();
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserSurveyUsageRepository userSurveyUsageRepository;
  @Mock private CaseInteractionRepository caseInteractionRepository;

  @InjectMocks UserService userService;

  private final String TEST_USER = "testUser";
  private final String TEST_USER_2 = "testUser2";
  private final String TEST_USER_3 = "testUser3";

  @Test
  public void canBeDeletedTrueWhenZeroCaseInteractionsSpecificUser() throws CTPException {
    when(userRepository.findByName(TEST_USER)).thenReturn(Optional.of(new User()));

    when(caseInteractionRepository.countAllByCcuserName(TEST_USER)).thenReturn(0);

    UserDTO result = userService.getUser(TEST_USER);

    assertTrue(result.isDeletable());
  }

  @Test
  public void canBeDeletedFalseWhenSomeCaseInteractionsSpecificUser() throws CTPException {
    when(userRepository.findByName(TEST_USER)).thenReturn(Optional.of(new User()));

    when(caseInteractionRepository.countAllByCcuserName(TEST_USER)).thenReturn(1);

    UserDTO result = userService.getUser(TEST_USER);

    assertFalse(result.isDeletable());
  }

  @Test
  public void canBeDeletedCorrectForMultipleUsers() throws CTPException {

    User test1 = User.builder().name(TEST_USER).build();
    User test2 = User.builder().name(TEST_USER_2).build();
    User test3 = User.builder().name(TEST_USER_3).build();

    List<User> users = List.of(test1, test2, test3);

    when(userRepository.findAll()).thenReturn(users);

    when(caseInteractionRepository.countAllByCcuserName(TEST_USER)).thenReturn(0);
    when(caseInteractionRepository.countAllByCcuserName(TEST_USER_2)).thenReturn(1);
    when(caseInteractionRepository.countAllByCcuserName(TEST_USER_3)).thenReturn(0);

    List<UserDTO> results = userService.getUsers();

    assertTrue(results.get(0).isDeletable());
    assertFalse(results.get(1).isDeletable());
    assertTrue(results.get(2).isDeletable());
  }
}

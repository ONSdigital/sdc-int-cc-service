package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

  @Test
  public void canBeDeletedTrueWhenZeroCaseInteractionsSpecificUser() throws CTPException {
    when(userRepository.findByName(TEST_USER)).thenReturn(Optional.of(new User()));

    when(caseInteractionRepository.countAllByCcuserName(TEST_USER)).thenReturn(0);

    UserDTO result = userService.getUser(TEST_USER);

    assertTrue(result.isCanBeDeleted());
  }

  @Test
  public void canBeDeletedFalseWhenSomeCaseInteractionsSpecificUser() throws CTPException {
    when(userRepository.findByName(TEST_USER)).thenReturn(Optional.of(new User()));

    when(caseInteractionRepository.countAllByCcuserName(TEST_USER)).thenReturn(1);

    UserDTO result = userService.getUser(TEST_USER);

    assertFalse(result.isCanBeDeleted());
  }

  @Test
  public void canBeDeletedTrueWhenZeroCaseInteractionsMultipleUsers() {}

  @Test
  public void canBeDeletedFalseWhenSomeCaseInteractionsMultipleUsers() {}
}

package uk.gov.ons.ctp.integration.contactcentresvc.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserIdentityHelperTest {
  private static final String USER_NAME = "mickey.mouse@ons.gov.uk";

  @Mock private UserRepository userRepo;
  @Mock private RoleRepository roleRepo;
  private AppConfig appConfig = mock(AppConfig.class, Mockito.RETURNS_DEEP_STUBS);

  private UserIdentityHelper userIdentityHelper;

  @BeforeEach
  public void init() {
    userIdentityHelper = new UserIdentityHelper(userRepo, roleRepo, appConfig);
    UserIdentityContext.set(USER_NAME);
  }

  @Test
  public void allowsNoRequestedSurveyWithPermissionAndNoSurveyUsage() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);

    when(userRepo.findByName(USER_NAME)).thenReturn(Optional.of(user));
    userIdentityHelper.assertUserPermission(PermissionType.SEARCH_CASES);
  }

  @Test
  public void allowsNoRequestedSurveyWithPermissionAndWithAnyOldSurveyUsage() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);

    when(userRepo.findByName(USER_NAME)).thenReturn(Optional.of(user));
    userIdentityHelper.assertUserPermission(PermissionType.SEARCH_CASES);
  }

  @Test
  public void allowsWithRequestedSurveyWithPermissionAndWithMatchingSurveyUsage()
      throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);
    Survey survey = Survey.builder().sampleDefinitionUrl("blahblah.social.json").build();

    when(userRepo.findByName(USER_NAME)).thenReturn(Optional.of(user));
    userIdentityHelper.assertUserPermission(survey, PermissionType.SEARCH_CASES);
  }

  @Test
  public void disallowsWithRequestedSurveyWithPermissionAndWithoutMatchingSurveyUsage()
      throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);
    Survey survey = Survey.builder().sampleDefinitionUrl("blahblah.fart.json").build();

    when(userRepo.findByName(USER_NAME)).thenReturn(Optional.of(user));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userIdentityHelper.assertUserPermission(survey, PermissionType.SEARCH_CASES);
            });
    assert (exception.getFault().equals(Fault.ACCESS_DENIED));
  }

  @Test
  public void disallowsWithRequestedSurveyWithoutPermission() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    Survey survey = Survey.builder().sampleDefinitionUrl("blahblah.social.json").build();

    when(userRepo.findByName(USER_NAME)).thenReturn(Optional.of(user));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userIdentityHelper.assertUserPermission(survey, PermissionType.SEARCH_CASES);
            });
    assert (exception.getFault().equals(Fault.ACCESS_DENIED));
  }

  private void addSurveyUsage(User user, SurveyType surveyType) {
    SurveyUsage surveyUsage =
        SurveyUsage.builder().id(UUID.randomUUID()).surveyType(surveyType).build();
    user.getSurveyUsages().add(surveyUsage);
  }

  private void addUserRoleWithPermission(User user, PermissionType permission) {
    Role role =
        Role.builder().id(UUID.randomUUID()).name("cleaner").permissions(new ArrayList<>()).build();
    Permission p = Permission.builder().id(UUID.randomUUID()).permissionType(permission).build();
    role.getPermissions().add(p);
    user.getUserRoles().add(role);
  }

  private User createUserWithoutPermissions(String userName) {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .name(userName)
            .active(true)
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .surveyUsages(new ArrayList<>())
            .build();
    return user;
  }
}

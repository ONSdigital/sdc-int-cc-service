package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

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
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@ExtendWith(MockitoExtension.class)
public class RBACServiceTest {
  private static final String USER_NAME = "mickey.mouse@ons.gov.uk";
  private static final UUID SURVEY_ID = UUID.fromString("55e7f61f-5e0e-45dd-b594-4ba8da2e8b60");

  @Mock private UserRepository userRepo;
  @Mock private RoleRepository roleRepo;
  @Mock private SurveyRepository surveyRepo;
  private AppConfig appConfig = mock(AppConfig.class, Mockito.RETURNS_DEEP_STUBS);

  private RBACService rbacService;

  @BeforeEach
  public void init() {
    rbacService = new RBACService(surveyRepo, userRepo, roleRepo, appConfig);
    UserIdentityContext.set(USER_NAME);
  }

  @Test
  public void allowsNoRequestedSurveyWithPermissionAndNoSurveyUsage() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);

    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    rbacService.assertUserPermission(PermissionType.SEARCH_CASES);
  }

  @Test
  public void allowsNoRequestedSurveyWithPermissionAndWithAnyOldSurveyUsage() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);

    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    rbacService.assertUserPermission(PermissionType.SEARCH_CASES);
  }

  @Test
  public void allowsWithRequestedSurveyWithPermissionAndWithMatchingSurveyUsage()
      throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);

    mockSurveyRepoCall("social");
    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    rbacService.assertUserPermission(SURVEY_ID, PermissionType.SEARCH_CASES);
  }

  @Test
  public void disallowsWithRequestedSurveyWithPermissionAndWithoutMatchingSurveyUsage()
      throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);

    mockSurveyRepoCall("business");
    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              rbacService.assertUserPermission(SURVEY_ID, PermissionType.SEARCH_CASES);
            });
    assert (exception.getFault().equals(Fault.ACCESS_DENIED));
  }

  @Test
  public void disallowsWithRequestedSurveyWithoutPermission() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    addSurveyUsage(user, SurveyType.SOCIAL);

    mockSurveyRepoCall("social");
    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              rbacService.assertUserPermission(SURVEY_ID, PermissionType.SEARCH_CASES);
            });
    assert (exception.getFault().equals(Fault.ACCESS_DENIED));
  }

  @Test
  public void disallowsDeactivatedUserWithPermission() throws CTPException {
    User user = createUserWithoutPermissions(USER_NAME);
    user.setActive(false);
    addUserRoleWithPermission(user, PermissionType.SEARCH_CASES);
    addSurveyUsage(user, SurveyType.SOCIAL);

    when(userRepo.findByIdentity(USER_NAME)).thenReturn(Optional.of(user));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              rbacService.assertUserPermission(SURVEY_ID, PermissionType.SEARCH_CASES);
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
            .identity(userName)
            .active(true)
            .adminRoles(new ArrayList<>())
            .userRoles(new ArrayList<>())
            .surveyUsages(new ArrayList<>())
            .build();
    return user;
  }

  private void mockSurveyRepoCall(String surveyStr) {
    Survey survey =
        Survey.builder()
            .id(SURVEY_ID)
            .sampleDefinitionUrl("blahblah." + surveyStr + ".json")
            .build();
    when(surveyRepo.getById(SURVEY_ID)).thenReturn(survey);
  }
}

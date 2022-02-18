package uk.gov.ons.ctp.integration.contactcentresvc.security;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.DummyUserConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@Slf4j
@Component
public class UserIdentityHelper {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final SurveyRepository surveyRepository;
  private final AppConfig appConfig;

  public UserIdentityHelper(
      SurveyRepository surveyRepository,
      UserRepository userRepository,
      RoleRepository roleRepository,
      AppConfig appConfig) {
    this.surveyRepository = surveyRepository;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.appConfig = appConfig;

    if (appConfig.getDummyUserConfig().isAllowed()) {
      log.error("*** SECURITY ALERT *** IF YOU SEE THIS IN PRODUCTION, SHUT DOWN IMMEDIATELY!!!");
    }
  }

  /**
   * In a few cases we wish to guard against a user performing a user/role related operation on
   * themselves ie granting themselves permissions. Security 101.
   *
   * @param userName the target user
   * @throws CTPException when the user is forbidden to operate on the their own account
   */
  public void assertNotSelfModification(String userName) throws CTPException {
    if (UserIdentityContext.get().equals(userName)) {
      throw new CTPException(
          Fault.ACCESS_DENIED, String.format("Self modification of user account not allowed"));
    }
  }

  /**
   * Asserts that the signed user exists etc etc and that they have permission to perform the
   * requested action
   *
   * @param permissionType the permission related to the required action
   * @throws CTPException they either have not logged in, do not exist, are inactive or do not have
   *     the necessary permission
   */
  @Transactional
  public void assertUserPermission(PermissionType permissionType) throws CTPException {
    assertUserPermission(null, permissionType);
  }

  /**
   * Asserts that the signed user exists etc etc and that they have permission to perform the
   * requested action for the requested survey
   *
   * @param permissionType the permission related to the required action
   * @throws CTPException they either have not logged in, do not exist, are inactive or do not have
   *     the necessary permission
   */
  @Transactional
  public void assertUserPermission(UUID surveyId, PermissionType permissionType)
      throws CTPException {

    if (userActingAsAllowedDummy()) {
      return;
    }

    User user = loadUser();
    Survey survey = (surveyId != null ? surveyRepository.getById(surveyId) : null);

    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        // survey not requested AND user has the specific permission
        if ((surveyId == null && permission.getPermissionType() == permissionType)
            // survey was requested AND user allowed to operate on that survey
            // AND user has the
            // permission
            || (surveyId != null
                && permission.getPermissionType() == permissionType
                && !user.getSurveyUsages().isEmpty()
                && user.getSurveyUsages().stream()
                    .anyMatch(
                        usu ->
                            usu.getSurveyType()
                                .equals(
                                    SurveyType.fromSampleDefinitionUrl(
                                        survey.getSampleDefinitionUrl()))))) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format(
            "User not authorised for activity %s for survey type %s",
            permissionType.name(),
            SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl())));
  }

  /**
   * A simple assertion with few uses where a specific permission is not required for an operation,
   * but we still want to assert that the user has logged in, exists and is active
   *
   * @throws CTPException
   */
  @Transactional
  public void assertUserValidAndActive() throws CTPException {

    if (userActingAsAllowedDummy()) {
      return;
    }
    loadUser();
  }

  /**
   * A special case assertion for the maintenance of user:role assignment For a non 'super' user, it
   * is required that they themselves are in the admin roles for the target role. An exception is
   * made if the user is a super user who will have the permission USER_ROLE_ADMIN. Otherwise
   * Catch-22.
   *
   * @param userName the identity of the user performing the action
   * @param targetRole the role being maintained
   * @param permissionType the specific permission
   * @throws CTPException not allowed
   */
  @Transactional
  public void assertAdminPermission(String roleName, PermissionType permissionType)
      throws CTPException {

    if (userActingAsAllowedDummy()) {
      return;
    }

    User user = loadUser();
    Role targetRole =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    boolean isAdminForRole = user.getAdminRoles().contains(targetRole);
    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        if (permission.getPermissionType() == PermissionType.USER_ROLE_ADMIN
            || (permission.getPermissionType() == permissionType && isAdminForRole)) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }

  /**
   * checks to see if the dummy user functionality is enabled through config and if so, is the
   * currently signed in user acting as the dummy
   *
   * @return true if the above
   */
  public boolean userActingAsAllowedDummy() {
    DummyUserConfig dummyUseConfig = appConfig.getDummyUserConfig();
    boolean dummyEnabled = false;
    if (dummyUseConfig.isAllowed()
        && UserIdentityContext.get().equals(dummyUseConfig.getUserName())) {
      log.warn("Dummy user is enabled - this should NOT be seen in a non dev/test environment");
      dummyEnabled = true;
    }
    return dummyEnabled;
  }

  /**
   * asserts that the user identity exists in context, the user exists in the db, and the user is
   * currently active
   *
   * @return the User if all the above
   * @throws CTPException one of the conditions above was not met
   */
  private User loadUser() throws CTPException {

    String principalIdentity = UserIdentityContext.get();

    if (StringUtils.isEmpty(principalIdentity)) {
      throw new CTPException(Fault.ACCESS_DENIED, String.format("User must be logged in"));
    }

    User user =
        userRepository
            .findByName(principalIdentity)
            .orElseThrow(
                () -> new CTPException(Fault.ACCESS_DENIED, "User unknown: " + principalIdentity));

    if (!user.isActive()) {
      throw new CTPException(
          Fault.ACCESS_DENIED, String.format("User %s no longer active", principalIdentity));
    }
    return user;
  }
}
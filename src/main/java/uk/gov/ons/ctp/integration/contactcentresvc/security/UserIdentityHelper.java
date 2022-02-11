package uk.gov.ons.ctp.integration.contactcentresvc.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.DummyUser;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@Slf4j
@Component
public class UserIdentityHelper {
  private final UserRepository userRepository;
  private final AppConfig appConfig;

  public UserIdentityHelper(UserRepository userRepository, AppConfig appConfig) {
    this.userRepository = userRepository;
    this.appConfig = appConfig;

    if (appConfig.getDummyUser().isAllowed()) {
      log.error("*** SECURITY ALERT *** IF YOU SEE THIS IN PRODUCTION, SHUT DOWN IMMEDIATELY!!!");
    }
  }

  public void assertNotSelfModification(String userName)
      throws CTPException {
    if (UserIdentityContext.get().equals(userName)) {
      throw new CTPException(
          Fault.BAD_REQUEST,
          String.format("Self modification of user account not allowed"));
    }
  }

  @Transactional
  public void assertUserPermission(PermissionType permissionType)
      throws CTPException {
    assertUserPermission(null, permissionType);
  }

  @Transactional
  public void assertUserPermission(Survey survey, PermissionType permissionType)
      throws CTPException {

    DummyUser dummyUser = appConfig.getDummyUser();
    if (dummyUser.isAllowed() && UserIdentityContext.get().equals(dummyUser.getSuperUserIdentity())) {
      // Dummy test super user is fully authorised, bypassing all security
      // This is **STRICTLY** for ease of dev/testing in non-production
      // environments
      return;
    }
    
    String principalIdentity = UserIdentityContext.get();

    if (StringUtils.isEmpty(principalIdentity)) {
      throw new CTPException(
          Fault.ACCESS_DENIED,
          String.format("User must be logged in"));
    }

    User user = userRepository
        .findByName(principalIdentity)
        .orElseThrow(() -> new CTPException(Fault.ACCESS_DENIED, "User unknown: " + principalIdentity));

    if (!user.isActive()) {
      throw new CTPException(
          Fault.ACCESS_DENIED,
          String.format("User %s no longer active", principalIdentity));
    }

    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        // survey not requested AND user has the specific permission
        if ((survey == null && permission.getPermissionType() == permissionType)
            // survey was requested AND user allowed to operate on that survey
            // AND user has the
            // permission
            || (survey != null
                && permission.getPermissionType() == permissionType
                && !user.getSurveyUsages().isEmpty()
                && user.getSurveyUsages().stream()
                    .anyMatch(
                        usu -> usu.getSurveyType()
                            .equals(
                                SurveyType.fromSampleDefinitionUrl(
                                    survey.getSampleDefinitionUrl()))))) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }

  /**
   * A special case assertion for the maintenance of user:role assignment For a
   * non 'super' user, it is required that they themselves are in the admin
   * roles for the target role. An exception is made if the user is a super user
   * who will have the permission USER_ROLE_ADMIN. Otherwise Catch-22.
   * 
   * @param userName the identity of the user performing the action
   * @param targetRole the role being maintained
   * @param permissionType the specific permission
   * @throws CTPException not allowed
   */
  @Transactional
  public void assertAdminPermission(Role targetRole, PermissionType permissionType)
      throws CTPException {
    DummyUser dummyUser = appConfig.getDummyUser();
    if (dummyUser.isAllowed() && UserIdentityContext.get().equals(dummyUser.getSuperUserIdentity())) {
      // Dummy test super user is fully authorised, bypassing all security
      // This is **STRICTLY** for ease of dev/testing in non-production
      // environments
      return;
    }

    String principalIdentity = UserIdentityContext.get();

    if (StringUtils.isEmpty(principalIdentity)) {
      throw new CTPException(
          Fault.ACCESS_DENIED,
          String.format("User must be logged in"));
    }

    User user = userRepository
        .findByName(principalIdentity)
        .orElseThrow(() -> new CTPException(Fault.ACCESS_DENIED, "User unknown: " + principalIdentity));

    if (!user.isActive()) {
      throw new CTPException(
          Fault.ACCESS_DENIED,
          String.format("User %s no longer active", principalIdentity));
    }

    boolean isAdminForRole = user.getAdminRoles().contains(targetRole);
    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        if (permission.getPermissionType() == PermissionType.USER_ROLE_ADMIN
            || (permission.getPermissionType() == permissionType
                && isAdminForRole)) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }
}

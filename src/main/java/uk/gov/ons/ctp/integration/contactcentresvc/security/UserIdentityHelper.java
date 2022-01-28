package uk.gov.ons.ctp.integration.contactcentresvc.security;

import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
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

  public void assertUserPermission(
      String userName, Survey survey, PermissionType permissionType) throws CTPException {
    DummyUser dummyUser = appConfig.getDummyUser();
    if (dummyUser.isAllowed() && userName.equals(dummyUser.getSuperUserIdentity())) {
      // Dummy test super user is fully authorised, bypassing all security
      // This is **STRICTLY** for ease of dev/testing in non-production
      // environments
      return;
    }

    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.ACCESS_DENIED, "User unknown"));

    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        // SUPER USER without a survey = GLOBAL super user (all permissions)
        if ((permission.getPermissionType() == PermissionType.SUPER_USER
            && permission.getSurvey() == null)
            // SUPER USER with a survey = super user only on the specified
            // survey
            || (permission.getPermissionType() == PermissionType.SUPER_USER
                && permission.getSurvey() != null
                && permission.getSurvey().getId().equals(survey.getId())
                // Otherwise, user must have specific activity/survey combo to
                // be authorised
                || (permission.getPermissionType() == permissionType
                    && (permission.getSurvey() == null
                        || (permission.getSurvey() != null
                            && permission.getSurvey().getId().equals(survey.getId())))))) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }

  public void assertGlobalUserPermission(String userName, PermissionType permissionType)
      throws CTPException {

    DummyUser dummyUser = appConfig.getDummyUser();
    if (dummyUser.isAllowed() && userName.equals(dummyUser.getSuperUserIdentity())) {
      // Dummy test super user is fully authorised, bypassing all security
      // This is **STRICTLY** for ease of dev/testing in non-production
      // environments
      return;
    }

    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.ACCESS_DENIED, "User unknown"));

    for (Role role : user.getUserRoles()) {
      for (Permission permission : role.getPermissions()) {
        // SUPER USER without a survey = GLOBAL super user (all permissions)
        if ((permission.getPermissionType() == PermissionType.SUPER_USER
            && permission.getSurvey() == null)
            // Otherwise, user must have specific activity to be authorised
            || (permission.getPermissionType() == permissionType)) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }

  public void assertAdminOrGlobalSuperPermission(String userName) throws CTPException {
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.ACCESS_DENIED, String.format("User unknown")));

    if (user.getAdminRoles().isEmpty()) {
      // If you're not admin of a group, you have to be super user
      assertGlobalUserPermission(userName, PermissionType.SUPER_USER);
    }
  }
}

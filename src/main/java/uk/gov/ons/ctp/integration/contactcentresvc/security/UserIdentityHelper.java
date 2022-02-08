package uk.gov.ons.ctp.integration.contactcentresvc.security;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.DummyUser;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
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

  @Transactional
  public void assertUserPermission(
      String userName, PermissionType permissionType) throws CTPException {
    assertUserPermission(userName, null, permissionType);
  }

  @Transactional
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
        // user is SUPER regardless of requested survey
        if ((permission.getPermissionType() == PermissionType.SUPER_USER)
            // survey not requested AND user has the specific permission
            || (survey == null && permission.getPermissionType() == permissionType)
            // survey was requested AND user allowed to operate on that survey AND user has the permission
            || (survey != null && permission.getPermissionType() == permissionType
                && !user.getSurveyUsages().isEmpty()
                && user.getSurveyUsages().stream().anyMatch(usu -> usu.getSurveyType()
                    .equals(SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl()))))) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }

  @Transactional
  public void assertAdminPermission(
      String userName, Role role, PermissionType permissionType) throws CTPException {
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
        // user is SUPER regardless of requested survey
        if ((permission.getPermissionType() == PermissionType.SUPER_USER)
            // survey not requested AND user has the specific permission
            || (survey == null && permission.getPermissionType() == permissionType)
            // survey was requested AND user allowed to operate on that survey AND user has the permission
            || (survey != null && permission.getPermissionType() == permissionType
                && !user.getSurveyUsages().isEmpty()
                && user.getSurveyUsages().stream().anyMatch(usu -> usu.getSurveyType()
                    .equals(SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl()))))) {
          return; // User is authorised
        }
      }
    }

    throw new CTPException(
        Fault.ACCESS_DENIED,
        String.format("User not authorised for activity %s", permissionType.name()));
  }
}

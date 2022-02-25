package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

@Slf4j
@Service
public class UserService {
  @Autowired
  MapperFacade mapper;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserSurveyUsageRepository userSurveyUsageRepository;
  @Autowired
  private RoleRepository roleRepository;

  @Transactional
  public UserDTO getUser(String userName) throws CTPException {

    log.debug("Entering getUser", kv("userName", userName));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public List<RoleDTO> getUsersRoles(String userName) throws CTPException {
    log.debug("Entering getUsersRoles");
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    return mapper.mapAsList(user.getUserRoles(), RoleDTO.class);
  }

  @Transactional
  public List<UserDTO> getUsers() throws CTPException {
    log.debug("Entering getUsers");
    return mapper.mapAsList(userRepository.findAll(), UserDTO.class);
  }

  @Transactional
  public UserDTO modifyUser(UserDTO userDTO) throws CTPException {

    log.debug("Entering modifyUser", kv("userName", userDTO.getName()));
    User user = userRepository.findByName(userDTO.getName())
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    user.setActive(userDTO.isActive());
    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO createUser(UserDTO userDTO) throws CTPException {

    log.debug("Entering createUser", kv("userName", userDTO.getName()));
    if (userRepository.findByName(userDTO.getName()).isPresent()) {
      throw new CTPException(Fault.BAD_REQUEST, "User with that name already exists");
    }

    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(userDTO.getName());

    if (userDTO.getSurveyUsages() != null) {
      user.setSurveyUsages(new ArrayList<>());
      for (SurveyUsageDTO surveyUsageDTO : userDTO.getSurveyUsages()) {
        SurveyUsage surveyUsage =
            userSurveyUsageRepository.findBySurveyType(surveyUsageDTO.getSurveyType())
                .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));
        user.getSurveyUsages().add(surveyUsage);
      }
    }

    if (userDTO.getUserRoles() != null) {
      user.setUserRoles(new ArrayList<>());
      for (String userRoleName : userDTO.getUserRoles()) {
        Role userRole = roleRepository.findByName(userRoleName).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User Role not found: " + userRoleName));
        user.getUserRoles().add(userRole);
      }
    }

    if (userDTO.getAdminRoles() != null) {
      user.setAdminRoles(new ArrayList<>());
      for (String adminRoleName : userDTO.getUserRoles()) {
        Role adminRole = roleRepository.findByName(adminRoleName).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Admin Role not found: " + adminRoleName));
        user.getAdminRoles().add(adminRole);
      }
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO addUserSurvey(String userName, SurveyType surveyType) throws CTPException {

    log.debug("Entering addUserSurvey", kv("userName", userName), kv("surveyType", surveyType));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    SurveyUsage surveyUsage = userSurveyUsageRepository.findBySurveyType(surveyType)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (!user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().add(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO removeUserSurvey(String userName, SurveyType surveyType) throws CTPException {

    log.debug("Entering removeUserSurvey", kv("userName", userName), kv("surveyType", surveyType));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    SurveyUsage surveyUsage = userSurveyUsageRepository.findBySurveyType(surveyType)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().remove(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO addUserRole(String userName, String roleName) throws CTPException {

    log.debug("Entering addUserRole", kv("userName", userName), kv("roleName", roleName));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (!user.getUserRoles().contains(role)) {
      user.getUserRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO removeUserRole(String userName, String roleName) throws CTPException {

    log.debug("Entering removeUserRole", kv("userName", userName), kv("roleName", roleName));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (user.getUserRoles().contains(role)) {
      user.getUserRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO addAdminRole(String userName, String roleName) throws CTPException {

    log.debug("Entering addAdminRole", kv("userName", userName), kv("roleName", roleName));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (!user.getAdminRoles().contains(role)) {
      user.getAdminRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }

  @Transactional
  public UserDTO removeAdminRole(String userName, String roleName) throws CTPException {

    log.debug("Entering removeAdminRole", kv("userName", userName), kv("roleName", roleName));
    User user = userRepository.findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (user.getAdminRoles().contains(role)) {
      user.getAdminRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return mapper.map(user, UserDTO.class);
  }
}

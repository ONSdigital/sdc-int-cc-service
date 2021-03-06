package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

@Slf4j
@Service
public class UserService {
  @Autowired MapperFacade mapper;
  @Autowired private UserRepository userRepository;
  @Autowired private UserSurveyUsageRepository userSurveyUsageRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserAuditRepository userAuditRepository;

  @Transactional
  public UserDTO getUser(String userIdentity) throws CTPException {

    log.debug("Entering getUser", kv("userIdentity", userIdentity));
    User user = findUserIfNotDeleted(userIdentity);

    return createDTO(user);
  }

  public String getUserIdentity(UUID userUUID) throws CTPException {

    log.debug("Entering getUser", kv("userUUID", userUUID));

    User user =
        userRepository
            .findById(userUUID)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found: " + userUUID));

    return user.getIdentity();
  }

  @Transactional
  public List<RoleDTO> getUsersRoles(String userIdentity) throws CTPException {
    log.debug("Entering getUsersRoles");
    User user = findUserIfNotDeleted(userIdentity);

    return mapper.mapAsList(user.getUserRoles(), RoleDTO.class);
  }

  @Transactional
  public List<UserDTO> getUsers() throws CTPException {
    log.debug("Entering getUsers");

    List<User> users = userRepository.findAll();
    List<UserDTO> userDTOs = new ArrayList<>();

    users.forEach(
        user -> {
          if (!user.isDeleted()) {
            userDTOs.add(createDTO(user));
          }
        });

    return userDTOs;
  }

  @Transactional
  public UserDTO modifyUser(UserDTO userDTO) throws CTPException {

    log.debug("Entering modifyUser", kv("userIdentity", userDTO.getIdentity()));
    User user = findUserIfNotDeleted(userDTO.getIdentity());

    user.setActive(userDTO.isActive());
    user.setForename(userDTO.getForename());
    user.setSurname(userDTO.getSurname());
    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO createUser(UserDTO userDTO) throws CTPException {

    log.debug("Entering createUser", kv("userIdentity", userDTO.getIdentity()));

    Optional<User> userOpt = userRepository.findByIdentity(userDTO.getIdentity());
    User user;
    if (userOpt.isPresent()) {
      user = userOpt.get();
      if (user.isDeleted()) {
        log.info("Undeleting user {}", user.getIdentity());
        user.setDeleted(false);
        // set other attributes to known state.
        // The UI will fill in the new values with additional calls.
        user.setActive(true);
        user.setUserRoles(Collections.emptyList());
        user.setAdminRoles(Collections.emptyList());
        user.setSurveyUsages(Collections.emptyList());
      } else {
        throw new CTPException(Fault.BAD_REQUEST, "User with that name already exists");
      }
    } else {
      user = new User();
      user.setId(UUID.randomUUID());
      user.setIdentity(userDTO.getIdentity());
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO addUserSurvey(String userIdentity, SurveyType surveyType) throws CTPException {

    log.debug(
        "Entering addUserSurvey", kv("userIdentity", userIdentity), kv("surveyType", surveyType));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    SurveyUsage surveyUsage =
        userSurveyUsageRepository
            .findBySurveyType(surveyType)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (!user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().add(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO removeUserSurvey(String userIdentity, SurveyType surveyType) throws CTPException {

    log.debug(
        "Entering removeUserSurvey",
        kv("userIdentity", userIdentity),
        kv("surveyType", surveyType));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    SurveyUsage surveyUsage =
        userSurveyUsageRepository
            .findBySurveyType(surveyType)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().remove(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO addUserRole(String userIdentity, String roleName) throws CTPException {

    log.debug("Entering addUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (!user.getUserRoles().contains(role)) {
      user.getUserRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO removeUserRole(String userIdentity, String roleName) throws CTPException {

    log.debug(
        "Entering removeUserRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (user.getUserRoles().contains(role)) {
      user.getUserRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO addAdminRole(String userIdentity, String roleName) throws CTPException {

    log.debug("Entering addAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (!user.getAdminRoles().contains(role)) {
      user.getAdminRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO removeAdminRole(String userIdentity, String roleName) throws CTPException {

    log.debug(
        "Entering removeAdminRole", kv("userIdentity", userIdentity), kv("roleName", roleName));
    User user = findUserIfNotDeleted(userIdentity);

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "Operation not allowed on an inactive user");
    }

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (user.getAdminRoles().contains(role)) {
      user.getAdminRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return createDTO(user);
  }

  @Transactional
  public UserDTO deleteUser(String userIdentity) throws CTPException {
    User user = findUserIfNotDeleted(userIdentity);

    UserDTO response = createDTO(user);

    if (!response.isDeletable()) {
      throw new CTPException(Fault.BAD_REQUEST, "User not deletable");
    }

    user.setDeleted(true);
    return response;
  }

  private UserDTO createDTO(User user) {
    UserDTO userDTO = mapper.map(user, UserDTO.class);
    userDTO.setDeletable(
        !userAuditRepository.existsByCcuserIdAndAuditType(user.getId(), AuditType.LOGIN));

    return userDTO;
  }

  private User findUserIfNotDeleted(String userIdentity) throws CTPException {
    User user =
        userRepository
            .findByIdentity(userIdentity)
            .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (user.isDeleted()) {
      throw new CTPException(
          Fault.BAD_REQUEST, String.format("User not found: " + user.getIdentity()));
    }

    return user;
  }
}

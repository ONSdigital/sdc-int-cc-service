package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@Slf4j
// @Timed
@RestController
@RequestMapping(value = "/users", produces = "application/json")
public class UserEndpoint {

  private MapperFacade mapper;

  private UserRepository userRepository;
  private UserSurveyUsageRepository userSurveyUsageRepository;
  private RoleRepository roleRepository;

  private UserIdentityHelper identityHelper;

  @Autowired
  public UserEndpoint(
      final UserRepository userRepository,
      final UserSurveyUsageRepository userSurveyUsageRepository,
      final RoleRepository roleRepository,
      final MapperFacade mapper,
      final UserIdentityHelper identityHelper) {
    this.userRepository = userRepository;
    this.userSurveyUsageRepository = userSurveyUsageRepository;
    this.roleRepository = roleRepository;
    this.mapper = mapper;
    this.identityHelper = identityHelper;
  }

  @GetMapping("/{userName}")
  @Transactional
  public ResponseEntity<UserDTO> getUserByName(
      @PathVariable(value = "userName") String userName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.READ_USER);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @GetMapping("/{userName}/permissions")
  @Transactional
  public ResponseEntity<Set<PermissionType>> getUsersPermissions(
      @PathVariable(value = "userName") String userName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    // All users need to access this so no permission assertion

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    if (!user.isActive()) {
      throw new CTPException(Fault.BAD_REQUEST, "User no longer active");
    }
    
    return ResponseEntity.ok(
        user.getUserRoles().stream()
            .map(r -> r.getPermissions())
            .flatMap(List::stream)
            .map(p -> p.getPermissionType())
            .collect(Collectors.toSet()));
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<UserDTO>> getUsers(
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.READ_USER);

    List<UserDTO> dtoList = mapper.mapAsList(userRepository.findAll(), UserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @PutMapping("/{userName}")
  @Transactional
  public ResponseEntity<UserDTO> modifyUser(
      @PathVariable(value = "userName") String userName,
      @RequestBody UserDTO userDTO,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.MODIFY_USER);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    user.setActive(userDTO.isActive());
    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<UserDTO> createUser(
      @RequestBody UserDTO userDTO, @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.CREATE_USER);

    if (userRepository.findByName(userDTO.getName()).isPresent()) {
      throw new CTPException(Fault.BAD_REQUEST, "User with that name already exists");
    }

    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(userDTO.getName());

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

//  @DeleteMapping("/{userName}")
//  @Transactional
//  public ResponseEntity<Void> deleteUserByName(
//      @PathVariable("userName") String userName,
//      @Value("#{request.getAttribute('principal')}") String principal)
//      throws CTPException {
//
//    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);
//
//    User user = userRepository
//        .findByName(userName)
//        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));
//    userRepository.delete(user);
//    return ResponseEntity.ok().build();
//  }

  @PatchMapping("/{userName}/addSurvey/{surveyType}")
  @Transactional
  public ResponseEntity<UserDTO> addUserSurvey(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "surveyType") SurveyType surveyType,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.USER_SURVEY_MAINTENANCE);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    SurveyUsage surveyUsage = userSurveyUsageRepository
        .findBySurveyType(surveyType)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (!user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().add(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PatchMapping("/{userName}/removeSurvey/{surveyType}")
  @Transactional
  public ResponseEntity<UserDTO> removeUserSurvey(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "surveyType") SurveyType surveyType,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.USER_ROLE_MAINTENANCE);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    SurveyUsage surveyUsage = userSurveyUsageRepository
        .findBySurveyType(surveyType)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "SurveyUsage not found"));

    if (user.getSurveyUsages().contains(surveyUsage)) {
      user.getSurveyUsages().remove(surveyUsage);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PatchMapping("/{userName}/addUserRole/{roleName}")
  @Transactional
  public ResponseEntity<UserDTO> addUserRole(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    identityHelper.assertAdminPermission(principal, role, PermissionType.USER_ROLE_MAINTENANCE);

    if (!user.getUserRoles().contains(role)) {
      user.getUserRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PatchMapping("/{userName}/removeUserRole/{roleName}")
  @Transactional
  public ResponseEntity<UserDTO> removeUserRole(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    identityHelper.assertAdminPermission(principal, role, PermissionType.USER_ROLE_MAINTENANCE);

    if (user.getUserRoles().contains(role)) {
      user.getUserRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PatchMapping("/{userName}/addAdminRole/{roleName}")
  @Transactional
  public ResponseEntity<UserDTO> addAdminRole(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {

    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (!user.getAdminRoles().contains(role)) {
      user.getAdminRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @PatchMapping("/{userName}/removeAdminRole/{roleName}")
  @Transactional
  public ResponseEntity<UserDTO> removeAdminRole(
      @PathVariable(value = "userName") String userName,
      @PathVariable(value = "roleName") String roleName,
      @Value("#{request.getAttribute('principal')}") String principal)
      throws CTPException {
    identityHelper.assertUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findByName(userName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    if (user.getAdminRoles().contains(role)) {
      user.getAdminRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }
}

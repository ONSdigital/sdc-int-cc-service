package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@Slf4j
// @Timed
@RestController
@RequestMapping(value = "/users", produces = "application/json")
public class UserEndpoint {

  private MapperFacade mapper;

  private UserRepository userRepository;
  private RoleRepository roleRepository;

  private UserIdentityHelper identityHelper;

  @Autowired
  public UserEndpoint(
      final UserRepository userRepository,
      final RoleRepository roleRepository,
      final MapperFacade mapper,
      final UserIdentityHelper identityHelper) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.mapper = mapper;
    this.identityHelper = identityHelper;
  }

  @GetMapping("/name/{userName}")
  @Transactional
  public ResponseEntity<UserDTO> getUserByName(
      @PathVariable(value = "userName") String userName,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {

    User user = userRepository
        .findByName(userName)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @GetMapping("/{userId}")
  @Transactional
  public ResponseEntity<UserDTO> getUserById(
      @PathVariable(value = "userId") UUID userId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {

    User user = userRepository
        .findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    return ResponseEntity.ok(mapper.map(user, UserDTO.class));
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<UserDTO>> getUsers(
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertAdminOrGlobalSuperPermission(principal);

    List<UserDTO> dtoList = mapper.mapAsList(userRepository.findAll(), UserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<Void> createUser(
      @RequestBody UserDTO userDTO,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(userDTO.getName());

    userRepository.saveAndFlush(user);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<String> deleteUserById(
      @PathVariable("userId") UUID userId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository.findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));
    userRepository.delete(user);
    return ResponseEntity.ok("Deleted user: " + principal);
  } 
  
  @PatchMapping("/{userId}/addUserRole/{roleId}")
  @Transactional
  public ResponseEntity<Void> addUserRole(
      @PathVariable(value = "userId") UUID userId,
      @PathVariable(value = "roleId") UUID roleId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository.findById(roleId).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));
    
    if (!user.getUserRoles().contains(role)) {
      user.getUserRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PatchMapping("/{userId}/removeUserRole/{roleId}")
  @Transactional
  public ResponseEntity<Void> removeUserRole(
      @PathVariable(value = "userId") UUID userId,
      @PathVariable(value = "roleId") UUID roleId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository.findById(roleId).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));
    
    if (user.getUserRoles().contains(role)) {
      user.getUserRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
  
  @PatchMapping("/{userId}/addAdminRole/{roleId}")
  @Transactional
  public ResponseEntity<Void> addAdminRole(
      @PathVariable(value = "userId") UUID userId,
      @PathVariable(value = "roleId") UUID roleId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository.findById(roleId).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));
    
    if (!user.getAdminRoles().contains(role)) {
      user.getAdminRoles().add(role);
    }

    userRepository.saveAndFlush(user);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PatchMapping("/{userId}/removeAdminRole/{roleId}")
  @Transactional
  public ResponseEntity<Void> removeAdminRole(
      @PathVariable(value = "userId") UUID userId,
      @PathVariable(value = "roleId") UUID roleId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    User user = userRepository
        .findById(userId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "User not found"));

    Role role = roleRepository.findById(roleId).orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));
    
    if (user.getAdminRoles().contains(role)) {
      user.getAdminRoles().remove(role);
    }

    userRepository.saveAndFlush(user);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}

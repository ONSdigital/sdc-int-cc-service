package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

/**
 * Example endpoints to access data from database.
 *
 * <p>This endpoint is purely for development use, and will be disabled or removed in future.
 * However for now it has development value in proving the database operations.
 *
 * <p>This demonstrates access to the database. Note however that it responds directly with entity
 * objects, which would not be a good idea for production code, not least because you need to avoid
 * lazy loading issues, and also because we need separation between the API and the database
 * internals.
 */
@RestController
@RequestMapping(value = "/data", produces = "application/json")
public class DataEndpoint {
  @Autowired CaseRepository caseRepo;
  @Autowired UserRepository userRepo;
  @Autowired RoleRepository roleRepo;
  @Autowired PermissionRepository permRepo;

  @GetMapping("/case")
  public ResponseEntity<List<Case>> findCases() {
    List<Case> result = caseRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/user")
  public ResponseEntity<List<User>> findUsers() {
    List<User> result = userRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @Transactional
  @GetMapping("/user/{user}/role")
  public ResponseEntity<List<String>> findUserRole(@PathVariable("user") String user) {
    User op = userRepo.findByName(user);
    List<String> roles = op.getUserRoles().stream().map(r -> r.getName()).collect(toList());
    return ResponseEntity.ok(roles);
  }

  @GetMapping("/role")
  public ResponseEntity<List<Role>> findRoles() {
    List<Role> result = roleRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/permission")
  public ResponseEntity<List<Permission>> findPermissions() {
    List<Permission> result = permRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @Transactional
  @GetMapping("/role/{role}/perm")
  public ResponseEntity<List<PermissionType>> findPermissionsForNamedRole(
      @PathVariable("role") String role) {
    Role result = roleRepo.findByName(role);
    List<PermissionType> response =
        result.getPermissions().stream().map(p -> p.getPermissionType()).collect(toList());
    return ResponseEntity.ok(response);
  }

  // list users who are an admin for a given role
  @Transactional
  @GetMapping("/role/{role}/admin")
  public ResponseEntity<List<String>> findAdminsForNamedRole(
      @PathVariable("role") String roleName) {
    Role role = roleRepo.findByName(roleName);

    List<User> allusers = userRepo.findAll();

    List<String> result =
        allusers.stream()
            .filter(u -> u.getAdminRoles().contains(role))
            .map(u -> u.getName())
            .collect(toList());

    return ResponseEntity.ok(result);
  }

  // add a role to a user
  @Transactional
  @PutMapping("/role/{role}/{user}")
  public ResponseEntity<String> makeUserRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    User user = userRepo.findByName(userName);
    List<Role> userRoles = user.getUserRoles();
    if (userRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Role " + roleName + " already exists for user " + userName);
    }
    userRoles.add(role);
    return ResponseEntity.ok("Role " + roleName + " added to user " + userName);
  }

  // add an admin role to a user
  @Transactional
  @PutMapping("/role/admin/{role}/{user}")
  public ResponseEntity<String> makeAdminRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    User user = userRepo.findByName(userName);
    List<Role> adminRoles = user.getAdminRoles();
    if (adminRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Admin Role " + roleName + " already exists for user " + userName);
    }
    adminRoles.add(role);
    return ResponseEntity.ok("Admin Role " + roleName + " added to user " + userName);
  }

  // remove a role from a user
  @Transactional
  @DeleteMapping("/role/{role}/{user}")
  public ResponseEntity<String> removeUserRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    User user = userRepo.findByName(userName);
    List<Role> userRoles = user.getUserRoles();
    if (!userRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Role " + roleName + " does not exist for user " + userName);
    }
    userRoles.remove(role);
    return ResponseEntity.ok("Role " + roleName + " removed from user " + userName);
  }

  // remove an admin role from a user
  @Transactional
  @DeleteMapping("/role/admin/{role}/{user}")
  public ResponseEntity<String> removeAdminRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    User user = userRepo.findByName(userName);
    List<Role> adminRoles = user.getAdminRoles();
    if (!adminRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Admin Role " + roleName + " does not exist for user " + userName);
    }
    adminRoles.remove(role);
    return ResponseEntity.ok("Admin Role " + roleName + " removed from user " + userName);
  }
}

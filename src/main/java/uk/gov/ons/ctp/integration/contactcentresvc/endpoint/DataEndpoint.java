package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Operator;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.OperatorRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;

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
  @Autowired OperatorRepository operatorRepo;
  @Autowired RoleRepository roleRepo;
  @Autowired PermissionRepository permRepo;

  @RequestMapping(value = "/case", method = RequestMethod.GET)
  public ResponseEntity<List<Case>> findCases() {
    List<Case> result = caseRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @RequestMapping(value = "/user", method = RequestMethod.GET)
  public ResponseEntity<List<Operator>> findUsers() {
    List<Operator> result = operatorRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @Transactional
  @RequestMapping(value = "/user/{user}/role", method = RequestMethod.GET)
  public ResponseEntity<List<String>> findUserRole(@PathVariable("user") String user) {
    Operator op = operatorRepo.findByName(user);
    List<String> roles = op.getMemberRoles().stream().map(r -> r.getName()).collect(toList());
    return ResponseEntity.ok(roles);
  }

  @RequestMapping(value = "/role", method = RequestMethod.GET)
  public ResponseEntity<List<Role>> findRoles() {
    List<Role> result = roleRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @RequestMapping(value = "/permission", method = RequestMethod.GET)
  public ResponseEntity<List<Permission>> findPermissions() {
    List<Permission> result = permRepo.findAll();
    return ResponseEntity.ok(result);
  }

  @Transactional
  @RequestMapping(value = "/role/{role}/perms", method = RequestMethod.GET)
  public ResponseEntity<List<PermissionType>> findPermissionsForNamedRole(
      @PathVariable("role") String role) {
    Role result = roleRepo.findByName(role);
    List<PermissionType> response =
        result.getPermissions().stream().map(p -> p.getPermissionType()).collect(toList());
    return ResponseEntity.ok(response);
  }

  // add a role to a user
  @Transactional
  @RequestMapping(value = "/role/{role}/{user}", method = RequestMethod.PUT)
  public ResponseEntity<String> makeMemberRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    Operator user = operatorRepo.findByName(userName);
    List<Role> memberRoles = user.getMemberRoles();
    if (memberRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Role " + roleName + " already exists for user " + userName);
    }
    memberRoles.add(role);
    return ResponseEntity.ok("Role " + roleName + " added to user " + userName);
  }

  // add an admin role to a user
  @Transactional
  @RequestMapping(value = "/role/admin/{role}/{user}", method = RequestMethod.PUT)
  public ResponseEntity<String> makeAdminRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    Operator user = operatorRepo.findByName(userName);
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
  @RequestMapping(value = "/role/{role}/{user}", method = RequestMethod.DELETE)
  public ResponseEntity<String> removeMemberRole(
      @PathVariable("role") String roleName, @PathVariable("user") String userName) {
    Role role = roleRepo.findByName(roleName);
    Operator user = operatorRepo.findByName(userName);
    List<Role> memberRoles = user.getMemberRoles();
    if (!memberRoles.contains(role)) {
      return ResponseEntity.badRequest()
          .body("Role " + roleName + " does not exist for user " + userName);
    }
    memberRoles.remove(role);
    return ResponseEntity.ok("Role " + roleName + " removed from user " + userName);
  }
}

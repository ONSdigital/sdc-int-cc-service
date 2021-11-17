package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.stream.Collectors;
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

/** Example endpoints to access data from database. */
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
        result.getPermissions().stream()
            .map(p -> p.getPermissionType())
            .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }
}

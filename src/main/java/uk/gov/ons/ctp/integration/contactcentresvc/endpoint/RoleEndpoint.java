package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.PermissionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.BasicUserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@Slf4j
// @Timed
@RestController
@RequestMapping(value = "/roles", produces = "application/json")
public class RoleEndpoint {

  private MapperFacade mapper;

  private RoleRepository roleRepository;

  private UserRepository userRepository;


  private SurveyRepository surveyRepository;

  private PermissionRepository permissionRepository;

  private UserIdentityHelper identityHelper;

  @Autowired
  public RoleEndpoint(
      final RoleRepository roleRepository,
      final UserRepository userRepository,
      final SurveyRepository surveyRepository,
      final PermissionRepository permissionRepository,
      final MapperFacade mapper,
      final UserIdentityHelper identityHelper) {
    this.roleRepository = roleRepository;
    this.userRepository = userRepository;
    this.surveyRepository = surveyRepository;
    this.permissionRepository = permissionRepository;
    this.mapper = mapper;
    this.identityHelper = identityHelper;
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<RoleDTO>> getRoles(
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    List<RoleDTO> dtoList = mapper.mapAsList(roleRepository.findAll(), RoleDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/{roleId}")
  @Transactional
  public ResponseEntity<RoleDTO> getRoleById(
      @PathVariable(value = "roleId") UUID roleId,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository
        .findById(roleId)
        .orElseThrow(
            () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @Transactional
  @GetMapping("/{role}/users")
  public ResponseEntity<List<BasicUserDTO>> findUsersForUserRole(@PathVariable("role") String roleName) {
    Role role = roleRepository.findByName(roleName);

    List<BasicUserDTO> dtoList = mapper.mapAsList(role.getUsers(), BasicUserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @Transactional
  @GetMapping("/{role}/admins")
  public ResponseEntity<List<BasicUserDTO>> findUsersForAdminRole(@PathVariable("role") String roleName) {
    Role role = roleRepository.findByName(roleName);

    List<BasicUserDTO> dtoList = mapper.mapAsList(role.getAdmins(), BasicUserDTO.class);
    return ResponseEntity.ok(dtoList);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<Void> createRole(
      @RequestBody RoleDTO roleDTO,
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    Role role = new Role();
    role.setId(UUID.randomUUID());
    role.setName(roleDTO.getName());

    roleRepository.saveAndFlush(role);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PatchMapping("/{roleId}/addPermission/{permissionType}")
  @Transactional
  public ResponseEntity<RoleDTO> addPermission(
      @PathVariable(value = "roleId") UUID roleId,
      @PathVariable(value = "permissionType") PermissionType permissionType,
      @RequestParam(value = "surveyId", required = false) UUID surveyId,
      @RequestAttribute(value = "principal") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository.findById(roleId).orElseThrow(
        () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Survey survey = null;
    if (surveyId != null) {
      survey = surveyRepository.findById(roleId).orElseThrow(
          () -> new CTPException(Fault.BAD_REQUEST, "Survey not found"));
    }

    Permission permission = permissionRepository.findByPermissionTypeAndRoleAndSurvey(permissionType, role, survey);
    if (permission == null) {
      permission = new Permission();
      permission.setId(UUID.randomUUID());
      permission.setRole(role);
      permission.setPermissionType(permissionType);
      if (survey != null) {
        permission.setSurvey(survey);
      }
      role.getPermissions().add(permission);
      roleRepository.saveAndFlush(role);
    }

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

  @PatchMapping("/{roleId}/removePermission/{permissionType}")
  @Transactional
  public ResponseEntity<RoleDTO> removePermission(
      @PathVariable(value = "roleId") UUID roleId,
      @PathVariable(value = "permissionType") PermissionType permissionType,
      @RequestParam(value = "surveyId", required = false) UUID surveyId,
      @RequestAttribute(value = "principal") String principal) throws CTPException {
    identityHelper.assertGlobalUserPermission(principal, PermissionType.SUPER_USER);

    Role role = roleRepository.findById(roleId).orElseThrow(
        () -> new CTPException(Fault.BAD_REQUEST, "Role not found"));

    Survey survey = null;
    if (surveyId != null) {
      survey = surveyRepository.findById(roleId).orElseThrow(
          () -> new CTPException(Fault.BAD_REQUEST, "Survey not found"));
    }

    Permission permission = permissionRepository.findByPermissionTypeAndRoleAndSurvey(permissionType, role, survey);
    if (permission != null) {
      log.info("Removing permission " + permission);
      permissionRepository.delete(permission);
      role.getPermissions().remove(permission);
      roleRepository.saveAndFlush(role);
    }

    return ResponseEntity.ok(mapper.map(role, RoleDTO.class));
  }

}

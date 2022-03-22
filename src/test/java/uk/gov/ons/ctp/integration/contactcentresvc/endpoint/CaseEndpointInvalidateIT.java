package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseEndpointInvalidateIT extends FullStackIntegrationTestBase {

  private static final UUID PRINCIPAL_USER =
      UUID.fromString("3f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID = UUID.fromString("5f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final String NOTE = "A note";

  @Autowired private CaseEndpointInvalidateIT.TransactionalOps txOps;
  @Autowired private CaseInteractionRepository interactionRepository;

  TestRestTemplate restTemplate;

  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setup() throws MalformedURLException {
    base = new URL("http://localhost:" + port);

    restTemplate = new TestRestTemplate(new RestTemplateBuilder());

    txOps.deleteAll();
    txOps.setupSurveyAndCollex();
  }

  @Test
  public void invalidateExistingCase() {
    var perms = List.of(PermissionType.INVALIDATE_CASE);
    Role role = txOps.createRole("invalidate_case", PRINCIPAL_USER, perms);
    txOps.createUser("principal.user@ext.ons.gov.uk", PRINCIPAL_USER, List.of(role), null);
    txOps.createCase(CASE_ID);

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "principal.user@ext.ons.gov.uk");

    InvalidateCaseDTO invalidateCaseDTO = new InvalidateCaseDTO();
    invalidateCaseDTO.setNote(NOTE);

    HttpEntity<?> requestEntity = new HttpEntity<>(invalidateCaseDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<ResponseDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/cases/" + CASE_ID + "/invalidate",
            HttpMethod.POST,
            requestEntity,
            ResponseDTO.class,
            params);

    List<CaseInteraction> interactions = interactionRepository.findAllByCazeId(CASE_ID);

    assertEquals(1, interactions.size());
    assertEquals(CASE_ID, interactions.get(0).getCaze().getId());
    assertEquals(NOTE, interactions.get(0).getNote());

    assertEquals(CASE_ID.toString(), response.getBody().getId());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;
    private UserSurveyUsageRepository userSurveyUsageRepository;
    private CaseRepository caseRepository;
    private RoleRepository roleRepository;
    private CaseInteractionRepository interactionRepository;
    private UserAuditRepository userAuditRepository;
    private CollectionExerciseRepository collectionExerciseRepository;
    private SurveyRepository surveyRepository;

    public TransactionalOps(
        UserRepository userRepo,
        UserSurveyUsageRepository userSurveyUsageRepository,
        RoleRepository roleRepository,
        CaseRepository caseRepository,
        CaseInteractionRepository interactionRepository,
        UserAuditRepository userAuditRepository,
        CollectionExerciseRepository collectionExerciseRepository,
        SurveyRepository surveyRepository) {
      this.userRepo = userRepo;
      this.userSurveyUsageRepository = userSurveyUsageRepository;
      this.roleRepository = roleRepository;
      this.caseRepository = caseRepository;
      this.interactionRepository = interactionRepository;
      this.userAuditRepository = userAuditRepository;
      this.surveyRepository = surveyRepository;
      this.collectionExerciseRepository = collectionExerciseRepository;
    }

    public void deleteAll() {
      userAuditRepository.deleteAll();
      interactionRepository.deleteAll();
      caseRepository.deleteAll();
      collectionExerciseRepository.deleteAll();
      surveyRepository.deleteAll();
      roleRepository.deleteAll();
      userRepo.deleteAll();
    }

    public void setupSurveyAndCollex() {
      Survey survey =
          Survey.builder()
              .id(UUID.fromString("230a9342-c54d-11eb-abce-4c3275913db5"))
              .name("survey")
              .sampleDefinitionUrl("/social.json")
              .sampleDefinition("{\"sample\":\"sample\"}")
              .build();
      CollectionExercise collectionExercise =
          CollectionExercise.builder()
              .id(UUID.fromString("430a9342-c54d-11eb-abce-4c3275913db5"))
              .startDate(LocalDateTime.now())
              .endDate(LocalDateTime.now())
              .survey(survey)
              .name("name")
              .reference("")
              .build();

      surveyRepository.save(survey);
      collectionExerciseRepository.save(collectionExercise);
    }

    public Case createCase(UUID caseId) {

      Case caze =
          Case.builder()
              .collectionExercise(
                  CollectionExercise.builder()
                      .id(UUID.fromString("430a9342-c54d-11eb-abce-4c3275913db5"))
                      .build())
              .id(caseId)
              .caseRef("10000000013")
              .createdAt(LocalDateTime.now())
              .lastUpdatedAt(LocalDateTime.now())
              .ccStatus(CCStatus.READY)
              .sample(new HashMap<>())
              .sampleSensitive(new HashMap<>())
              .build();

      caseRepository.save(caze);
      return caze;
    }

    public Role createRole(String name, UUID id, List<PermissionType> permTypes) {
      permTypes = permTypes == null ? new ArrayList<>() : permTypes;
      Role role = Role.builder().id(id).name(name).permissions(new ArrayList<>()).build();

      permTypes.stream()
          .forEach(
              type -> {
                addPermission(role, type);
              });
      return roleRepository.save(role);
    }

    public Role addPermission(Role role, PermissionType type) {
      Permission p =
          Permission.builder().id(UUID.randomUUID()).permissionType(type).role(role).build();
      role.getPermissions().add(p);
      roleRepository.save(role);
      return role;
    }

    public User createUser(String name, UUID id, List<Role> userRoles, List<Role> adminRoles) {

      SurveyUsage surveyUsage = userSurveyUsageRepository.findBySurveyType(SurveyType.SOCIAL).get();
      User user =
          User.builder()
              .id(id)
              .identity(name)
              .userRoles(userRoles == null ? Collections.emptyList() : userRoles)
              .adminRoles(adminRoles == null ? Collections.emptyList() : adminRoles)
              .surveyUsages(List.of(surveyUsage))
              .build();
      userRepo.save(user);

      return user;
    }
  }
}

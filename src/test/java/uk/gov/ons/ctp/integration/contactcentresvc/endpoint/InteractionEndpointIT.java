package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;

public class InteractionEndpointIT extends FullStackIntegrationTestBase {

  private static final UUID TARGET_USER = UUID.fromString("4f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID PRINCIPAL_USER =
      UUID.fromString("3f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID = UUID.fromString("5f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID_2 = UUID.fromString("6f27ee97-7ba7-4979-b9e8-bfe3063b41e8");

  @Autowired private InteractionEndpointIT.TransactionalOps txOps;

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
  public void getCorrectCaseInteractionsByUser() {
    var perms = List.of(PermissionType.READ_USER_INTERACTIONS);
    Role role = txOps.createRole("interaction_reader", TARGET_USER, perms);
    User otherUser =
        txOps.createUser("principal.user@ext.ons.gov.uk", TARGET_USER, List.of(role), null);
    User targetUser = txOps.createUser("target.user@ext.ons.gov.uk", PRINCIPAL_USER);
    Case case1 = txOps.createCase(CASE_ID);
    Case case2 = txOps.createCase(CASE_ID_2);

    txOps.createInteraction(case1, otherUser, LocalDateTime.now());
    txOps.createInteraction(case2, otherUser, LocalDateTime.now());

    txOps.createInteraction(case1, targetUser, LocalDateTime.of(2022, 1, 1, 1, 1));
    txOps.createInteraction(case2, targetUser, LocalDateTime.of(2021, 1, 1, 1, 1));
    txOps.createInteraction(case1, targetUser, LocalDateTime.of(2022, 3, 3, 3, 3));

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "principal.user@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<List<UsersCaseInteractionDTO>> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/interactions/user/target.user@ext.ons.gov.uk",
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<UsersCaseInteractionDTO>>() {},
            params);

    assertEquals(3, response.getBody().size());
    assertEquals(
        LocalDateTime.of(2022, 3, 3, 3, 3), response.getBody().get(0).getCreatedDateTime());
    assertEquals(
        LocalDateTime.of(2022, 1, 1, 1, 1), response.getBody().get(1).getCreatedDateTime());
    assertEquals(
        LocalDateTime.of(2021, 1, 1, 1, 1), response.getBody().get(2).getCreatedDateTime());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;
    private CaseRepository caseRepository;
    private RoleRepository roleRepository;
    private CaseInteractionRepository interactionRepository;
    private UserAuditRepository userAuditRepository;
    private CollectionExerciseRepository collectionExerciseRepository;
    private SurveyRepository surveyRepository;

    public TransactionalOps(
        UserRepository userRepo,
        RoleRepository roleRepository,
        CaseRepository caseRepository,
        CaseInteractionRepository interactionRepository,
        UserAuditRepository userAuditRepository,
        CollectionExerciseRepository collectionExerciseRepository,
        SurveyRepository surveyRepository) {
      this.userRepo = userRepo;
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
      // collectionExerciseRepository.deleteAll();
      // surveyRepository.deleteAll();
      roleRepository.deleteAll();
      userRepo.deleteAll();
    }

    public void createInteraction(Case caze, User user, LocalDateTime localDateTime) {

      CaseInteraction interaction =
          CaseInteraction.builder()
              .ccuser(user)
              .caze(caze)
              .type(CaseInteractionType.CASE_NOTE_ADDED)
              .note("note")
              .createdDateTime(localDateTime)
              .build();
      interactionRepository.save(interaction);
    }

    public User createUser(String name, UUID id) {
      return createUser(name, id, null, null);
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
      User user =
          User.builder()
              .id(id)
              .identity(name)
              .userRoles(userRoles == null ? Collections.emptyList() : userRoles)
              .adminRoles(adminRoles == null ? Collections.emptyList() : adminRoles)
              .build();
      userRepo.save(user);

      return user;
    }
  }
}

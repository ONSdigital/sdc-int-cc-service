package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.domain.RefusalType;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.event.CaseUpdateEventReceiver;
import uk.gov.ons.ctp.integration.contactcentresvc.event.UacUpdateEventReceiver;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CCStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;

/**
 * Separate class that can create/update database items and commit the results, so that tests can
 * manipulate the database state.
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalOps {
  private UserRepository userRepo;
  private UserSurveyUsageRepository userSurveyUsageRepository;
  private CaseRepository caseRepository;
  private RoleRepository roleRepository;
  private PermissionRepository permissionRepository;
  private UacRepository uacRepository;
  private CaseInteractionRepository interactionRepository;
  private UserAuditRepository userAuditRepository;
  private CollectionExerciseRepository collectionExerciseRepository;
  private SurveyRepository surveyRepository;
  private EventToSendRepository eventToSendRepository;
  private CaseUpdateEventReceiver target;
  private UacUpdateEventReceiver uacUpdateEventReceiver;

  public TransactionalOps(
      UserRepository userRepo,
      UserSurveyUsageRepository userSurveyUsageRepository,
      RoleRepository roleRepository,
      PermissionRepository permissionRepository,
      UacRepository uacRepository,
      CaseRepository caseRepository,
      CaseInteractionRepository interactionRepository,
      UserAuditRepository userAuditRepository,
      CollectionExerciseRepository collectionExerciseRepository,
      SurveyRepository surveyRepository,
      EventToSendRepository eventToSendRepository,
      CaseUpdateEventReceiver target,
      UacUpdateEventReceiver uacUpdateEventReceiver) {
    this.userRepo = userRepo;
    this.userSurveyUsageRepository = userSurveyUsageRepository;
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.uacRepository = uacRepository;
    this.caseRepository = caseRepository;
    this.interactionRepository = interactionRepository;
    this.userAuditRepository = userAuditRepository;
    this.collectionExerciseRepository = collectionExerciseRepository;
    this.surveyRepository = surveyRepository;
    this.eventToSendRepository = eventToSendRepository;
    this.target = target;
    this.uacUpdateEventReceiver = uacUpdateEventReceiver;
  }

  public void deleteAll() {
    userAuditRepository.deleteAll();
    userSurveyUsageRepository.deleteAll();
    interactionRepository.deleteAll();
    uacRepository.deleteAll();
    caseRepository.deleteAll();
    userRepo.deleteAll();
    roleRepository.deleteAll();
    permissionRepository.deleteAll();
    collectionExerciseRepository.deleteAll();
    surveyRepository.deleteAll();
    eventToSendRepository.deleteAll();
  }

  public User createSurveyUser(String name, UUID id, List<Role> userRoles, List<Role> adminRoles) {
    User user = createUser(name, id, userRoles, adminRoles);

    SurveyUsage surveyUsage = new SurveyUsage(UUID.randomUUID(), SurveyType.SOCIAL, List.of(user));
    userSurveyUsageRepository.saveAndFlush(surveyUsage);

    user.setSurveyUsages(List.of(surveyUsage));
    userRepo.save(user);

    return user;
  }

  public User createDeletedUser(String name, UUID id) {
    User user = createUser(name, id);
    user.setDeleted(true);
    userRepo.save(user);
    return user;
  }

  public User createUser(String name, UUID id) {
    return createUser(name, id, null, null);
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

  public User findUser(String identity) {
    return userRepo.findByIdentity(identity).orElse(null);
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

  public void removePermission(Role role, PermissionType type) {
    Optional<Permission> p = permissionRepository.findByPermissionTypeAndRole(type, role);
    permissionRepository.delete(p.get());
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

  public Survey createSurvey(UUID id, String name, String sampleDefinitionURL) {
    Survey survey =
        Survey.builder()
            .id(id)
            .name(name)
            .sampleDefinitionUrl(sampleDefinitionURL)
            .sampleDefinition("{}")
            .build();
    surveyRepository.save(survey);

    return survey;
  }

  public CollectionExercise createCollex(Survey survey, UUID id) {
    CollectionExercise cx =
        CollectionExercise.builder()
            .id(id)
            .survey(survey)
            .name("gregory")
            .reference("MVP012021")
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusDays(1))
            .build();
    collectionExerciseRepository.save(cx);
    return cx;
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

  public void createCase(CollectionExercise collectionExercise, UUID id) {

    Map<String, String> sample = new HashMap<>();
    sample.put(CaseUpdate.ATTRIBUTE_UPRN, "1234");
    sample.put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1, "1 Street Name");
    sample.put(CaseUpdate.ATTRIBUTE_TOWN_NAME, "TOWN");
    sample.put(CaseUpdate.ATTRIBUTE_POSTCODE, "PO57 6DE");
    sample.put(CaseUpdate.ATTRIBUTE_REGION, Region.E.name());
    sample.put(CaseUpdate.ATTRIBUTE_COHORT, "1");
    sample.put(CaseUpdate.ATTRIBUTE_QUESTIONNAIRE, "1");
    sample.put(CaseUpdate.ATTRIBUTE_SAMPLE_UNIT_REF, "unit");

    Case collectionCase =
        Case.builder()
            .id(id)
            .collectionExercise(collectionExercise)
            .caseRef("1")
            .sample(sample)
            .ccStatus(CCStatus.READY)
            .sampleSensitive(new HashMap<>())
            .createdAt(LocalDateTime.parse("2021-12-01T00:00:00.000"))
            .invalid(false)
            .lastUpdatedAt(LocalDateTime.parse("2021-12-01T00:00:00.000"))
            .refusalReceived(RefusalType.EXTRAORDINARY_REFUSAL)
            .build();
    caseRepository.saveAndFlush(collectionCase);
  }

  public void createSkeletonCase(CollectionExercise collectionExercise, UUID id) {

    Case caze =
        Case.builder()
            .id(id)
            .collectionExercise(collectionExercise)
            .ccStatus(CCStatus.PENDING)
            .caseRef("")
            .lastUpdatedAt(LocalDateTime.parse("9999-01-01T00:00:00.000"))
            .createdAt(LocalDateTime.parse("9999-01-01T00:00:00.000"))
            .sample(new HashMap<>())
            .sampleSensitive(new HashMap<>())
            .build();
    caseRepository.save(caze);
  }

  public void createEvent(LocalDateTime created) {
    EventToSend event =
        EventToSend.builder()
            .id(UUID.randomUUID())
            .payload("payload")
            .type(TopicType.EQ_LAUNCH.name())
            .createdDateTime(created)
            .build();

    eventToSendRepository.save(event);
  }

  public void acceptEvent(CaseEvent event) throws CTPException {
    target.acceptEvent(event);
  }

  public void acceptEvent(UacEvent event) throws CTPException {
    uacUpdateEventReceiver.acceptEvent(event);
  }

  public void verifyNormalUserAndRole(String userName, String roleName) {
    Optional<User> user = userRepo.findByIdentity(userName);
    Optional<Role> role = roleRepository.findByName(roleName);
    assertEquals(role.get(), user.get().getUserRoles().get(0));
    assertEquals(user.get(), role.get().getUsers().get(0));
  }

  public void verifyAdminUserAndRole(String userName, String roleName) {
    Optional<User> user = userRepo.findByIdentity(userName);
    Optional<Role> role = roleRepository.findByName(roleName);
    assertEquals(role.get(), user.get().getAdminRoles().get(0));
    assertEquals(user.get(), role.get().getAdmins().get(0));
  }
}

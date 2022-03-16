package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.CASE_NOTE_ADDED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.REFUSAL_REQUESTED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_ANSWERED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.DummyUserConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;

@ExtendWith(MockitoExtension.class)
public class InteractionServiceImplTest {

  @Mock AppConfig appConfig;

  @Mock CaseInteractionRepository interactionRepository;

  @Mock UserRepository userRepository;

  @Mock RBACService rbacService;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks InteractionService interactionService = new InteractionService();

  @Captor private ArgumentCaptor<CaseInteraction> interactionCaptor;

  private static final UUID USER_ID = UUID.fromString("382a8474-479c-11ec-a052-4c3275913db5");
  private static final String USER_IDENTITY = "philip.whiles@ext.ons.gov.uk";

  @Test
  public void shouldSaveValidInteraction() throws CTPException {
    mockUser();
    CaseInteractionRequestDTO caseInteractionRequestDTO =
        FixtureHelper.loadClassFixtures(CaseInteractionRequestDTO[].class).get(0);

    interactionService.saveCaseInteraction(USER_ID, caseInteractionRequestDTO);

    CaseInteraction expectedInteraction =
        mapperFacade.map(caseInteractionRequestDTO, CaseInteraction.class);
    expectedInteraction.setCcuser(User.builder().id(USER_ID).build());
    expectedInteraction.setCaze(Case.builder().id(USER_ID).build());

    verify(interactionRepository).saveAndFlush(interactionCaptor.capture());

    CaseInteraction actualInteraction = interactionCaptor.getValue();
    assertEquals(expectedInteraction.getType(), actualInteraction.getType());
    assertEquals(expectedInteraction.getSubtype(), actualInteraction.getSubtype());
    assertEquals(expectedInteraction.getNote(), actualInteraction.getNote());
    assertEquals(expectedInteraction.getCaze().getId(), actualInteraction.getCaze().getId());
    assertEquals(expectedInteraction.getCcuser().getId(), actualInteraction.getCcuser().getId());
  }

  @Test
  public void shouldRejectInvalidInteraction() throws CTPException {
    mockUser();
    CaseInteractionRequestDTO inconsistentDto =
        CaseInteractionRequestDTO.builder()
            .type(REFUSAL_REQUESTED)
            .subtype(CALL_ANSWERED)
            .note("a note")
            .build();

    assertThrows(
        CTPException.class, () -> interactionService.saveCaseInteraction(USER_ID, inconsistentDto));
  }

  @Test
  public void shouldRejectNullSubtypeWhenExpected() throws CTPException {
    mockUser();
    CaseInteractionRequestDTO inconsistentDto =
        CaseInteractionRequestDTO.builder()
            .type(REFUSAL_REQUESTED)
            .subtype(null)
            .note("a note")
            .build();

    assertThrows(
        CTPException.class, () -> interactionService.saveCaseInteraction(USER_ID, inconsistentDto));
  }

  @Test
  public void shouldAcceptInteractionNotNeedingSubtype() throws CTPException {
    mockUser();
    CaseInteractionRequestDTO dto =
        CaseInteractionRequestDTO.builder()
            .type(CASE_NOTE_ADDED)
            .subtype(null)
            .note("a note")
            .build();

    interactionService.saveCaseInteraction(USER_ID, dto);
    verify(interactionRepository).saveAndFlush(interactionCaptor.capture());
  }

  @Test
  public void shouldReturnUserInteractionsInCorrectOrder() throws CTPException {

    User user = User.builder().id(USER_ID).build();

    when(userRepository.findByIdentity(USER_IDENTITY)).thenReturn(Optional.of(user));

    List<CaseInteraction> interactions =
        List.of(
            createCaseInteraction(user, LocalDateTime.of(2022, 1, 1, 1, 1, 1, 0)),
            createCaseInteraction(user, LocalDateTime.of(2021, 1, 1, 1, 1, 1, 0)),
            createCaseInteraction(user, LocalDateTime.of(2022, 3, 1, 1, 1, 1, 0)));

    when(interactionRepository.findAllByCcuserId(USER_ID)).thenReturn(interactions);

    List<UsersCaseInteractionDTO> response =
        interactionService.getAllCaseInteractionsForUser(USER_IDENTITY);

    assertTrue(response.get(0).getCreatedDateTime().isAfter(response.get(1).getCreatedDateTime()));
    assertTrue(response.get(1).getCreatedDateTime().isAfter(response.get(2).getCreatedDateTime()));
  }

  private CaseInteraction createCaseInteraction(User user, LocalDateTime localDateTime) {

    Survey survey = Survey.builder().sampleDefinitionUrl("/social.json").build();
    CollectionExercise collectionExercise = CollectionExercise.builder().survey(survey).build();
    Case caze =
        Case.builder()
            .collectionExercise(collectionExercise)
            .id(UUID.randomUUID())
            .caseRef("10000000013")
            .build();

    return CaseInteraction.builder()
        .caze(caze)
        .ccuser(user)
        .type(REFUSAL_REQUESTED)
        .subtype(CALL_ANSWERED)
        .note("note")
        .createdDateTime(localDateTime)
        .build();
  }

  private void mockUser() {
    // mock the use of a dummy user - user interactions will then be recorded
    // against that user. The alternative would be to set the UserIdentityContext with
    // a test user name, and to mock the userRepo fetch of that user
    DummyUserConfig dummyUserConfig = new DummyUserConfig();
    dummyUserConfig.setAllowed(true);
    dummyUserConfig.setUserId(USER_ID);
    dummyUserConfig.setUserIdentity(USER_IDENTITY);
    when(rbacService.userActingAsAllowedDummy()).thenReturn(true);
    when(appConfig.getDummyUserConfig()).thenReturn(dummyUserConfig);
  }
}

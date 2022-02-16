package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@ExtendWith(MockitoExtension.class)
public class InteractionServiceImplTest {

  @Mock AppConfig appConfig;

  @Mock CaseInteractionRepository interactionRepository;

  @Mock UserRepository userRepository;

  @Mock UserIdentityHelper userIdentityHelper;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks InteractionService interactionService = new InteractionService();

  @Captor private ArgumentCaptor<CaseInteraction> interactionCaptor;

  private final UUID USER_ID = UUID.fromString("382a8474-479c-11ec-a052-4c3275913db5");
  private final String USER_NAME = "philip.whiles@ext.ons.gov.uk";

  @Test
  public void saveInteraction() throws CTPException {
    CaseInteractionRequestDTO caseInteractionRequestDTO =
        FixtureHelper.loadClassFixtures(CaseInteractionRequestDTO[].class).get(0);
    // mock the use of a dummy user - user interactions will then be recorded
    // against that user. The alternative would be to set the UserIdentityContext with
    // a test user name, and to mock the userRepo fetch of that user
    DummyUserConfig dummyUserConfig = new DummyUserConfig();
    dummyUserConfig.setAllowed(true);
    dummyUserConfig.setUserId(USER_ID);
    dummyUserConfig.setUserName(USER_NAME);
    when(userIdentityHelper.userActingAsAllowedDummy()).thenReturn(true);
    when(appConfig.getDummyUserConfig()).thenReturn(dummyUserConfig);

    interactionService.saveCaseInteraction(USER_ID, caseInteractionRequestDTO);

    CaseInteraction expectedInteraction =
        mapperFacade.map(caseInteractionRequestDTO, CaseInteraction.class);
    expectedInteraction.setCcuser(User.builder().id(USER_ID).build());
    expectedInteraction.setCaseId(USER_ID);

    verify(interactionRepository, times(1)).saveAndFlush(interactionCaptor.capture());

    CaseInteraction actualInteraction = interactionCaptor.getValue();
    assertEquals(expectedInteraction.getType(), actualInteraction.getType());
    assertEquals(expectedInteraction.getSubtype(), actualInteraction.getSubtype());
    assertEquals(expectedInteraction.getNote(), actualInteraction.getNote());
    assertEquals(expectedInteraction.getCaseId(), actualInteraction.getCaseId());
    assertEquals(expectedInteraction.getCcuser().getId(), actualInteraction.getCcuser().getId());
  }
}

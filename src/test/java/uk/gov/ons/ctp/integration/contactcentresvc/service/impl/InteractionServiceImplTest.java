package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;

@ExtendWith(MockitoExtension.class)
public class InteractionServiceImplTest {

  @Mock AppConfig appConfig;

  @Mock CaseInteractionRepository repository;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks InteractionServiceImpl interactionService = new InteractionServiceImpl();

  @Captor private ArgumentCaptor<CaseInteraction> interactionCaptor;

  private final UUID uuid = UUID.fromString("382a8474-479c-11ec-a052-4c3275913db5");

  @Test
  public void saveInteraction() {
    CaseInteractionDTO caseInteractionDTO =
        FixtureHelper.loadClassFixtures(CaseInteractionDTO[].class).get(0);

    interactionService.saveCaseInteraction(uuid, caseInteractionDTO);

    CaseInteraction expectedInteraction =
        mapperFacade.map(caseInteractionDTO, CaseInteraction.class);
    expectedInteraction.setCcuser(User.builder().id(uuid).build());
    expectedInteraction.setCaseId(uuid);

    verify(repository, times(1)).saveAndFlush(interactionCaptor.capture());

    CaseInteraction actualInteraction = interactionCaptor.getValue();
    assertEquals(expectedInteraction.getType(), actualInteraction.getType());
    assertEquals(expectedInteraction.getSubtype(), actualInteraction.getSubtype());
    assertEquals(expectedInteraction.getNote(), actualInteraction.getNote());
    assertEquals(expectedInteraction.getCaseId(), actualInteraction.getCaseId());
    assertEquals(expectedInteraction.getCcuser().getId(), actualInteraction.getCcuser().getId());
  }
}

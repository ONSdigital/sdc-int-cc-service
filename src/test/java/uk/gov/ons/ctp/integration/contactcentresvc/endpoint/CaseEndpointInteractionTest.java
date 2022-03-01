package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/** Contact Centre Endpoint Unit tests. This class tests the POST case interaction endpoint */
@ExtendWith(MockitoExtension.class)
public class CaseEndpointInteractionTest {

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;
  @Mock RBACService rbacService;

  @Mock InteractionService interactionService;

  private MockMvc mockMvc;

  private UUID uuid = UUID.randomUUID();

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @BeforeEach
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void saveCaseInteraction_validInteraction() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("Valid");
    Mockito.when(interactionService.saveCaseInteraction(eq(uuid), any()))
        .thenReturn(ResponseDTO.builder().build());

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isOk());
  }

  @Test
  public void saveCaseInteraction_InvalidInteractionType() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("InvalidType");

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void saveCaseInteraction_InvalidSubInteractionType() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("InvalidSubtype");

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void saveCaseInteraction_NonExplicitInteractionType() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("NonExplicitInteraction");

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void saveCaseInteraction_WrongSubTypeForInteraction() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("WrongSubtype");

    when(interactionService.saveCaseInteraction(eq(uuid), any()))
        .thenThrow(new CTPException(Fault.VALIDATION_FAILED));

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void saveCaseInteraction_AddCaseNote() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("CaseNote");

    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/interaction", json.toString()));
    actions.andExpect(status().isOk());
  }
}

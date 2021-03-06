package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/** Contact Centre Data Endpoint Unit tests */
@ExtendWith(MockitoExtension.class)
public class CaseEndpointCaseLaunchTest {

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  @Mock InteractionService interactionService;

  @Mock RBACService rbacService;

  @Autowired private MockMvc mockMvc;

  private UUID uuid = UUID.randomUUID();

  private CaseInteractionRequestDTO interactionDTO =
      CaseInteractionRequestDTO.builder()
          .type(CaseInteractionType.TELEPHONE_CAPTURE_STARTED)
          .build();

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
  public void getLaunchURL_ValidInvocation() throws Exception {
    String fakeResponse = "{\"url\": \"https://www.google.co.uk/search?q=FAKE\"}";
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    when(caseService.getLaunchURLForCaseId(any(), any())).thenReturn(fakeResponse);

    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=12345"));
    actions.andExpect(status().isOk());

    // Check that the url is as expected. Note that MockMvc (or some component in the chain) escapes
    // all double quotes
    String responseUrl = actions.andReturn().getResponse().getContentAsString();
    String expectedUrl = "\"{\\\"url\\\": \\\"https://www.google.co.uk/search?q=FAKE\\\"}\"";
    assertEquals(expectedUrl, responseUrl);

    verify(interactionService, times(1)).saveCaseInteraction(uuid, interactionDTO);
  }

  @Test
  public void getLaunchURL_BadCaseId() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/123456789/launch?agentId=12345"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdMissingAgentTest() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdBadAgent() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=ABC45"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void shouldRejectServiceBadRequestException() throws Exception {
    CTPException ex = new CTPException(Fault.BAD_REQUEST, "a message");
    when(caseService.getLaunchURLForCaseId(any(), any())).thenThrow(ex);
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=12345"));
    actions
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("a message")));
  }

  @Test
  public void shouldRejectServiceAcceptedUnableToProcessException() throws Exception {
    CTPException ex = new CTPException(Fault.ACCEPTED_UNABLE_TO_PROCESS, "a message");
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    when(caseService.getLaunchURLForCaseId(any(), any())).thenThrow(ex);
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=12345"));
    actions
        .andExpect(status().isAccepted())
        .andExpect(content().string(containsString("a message")));
  }

  @Test
  public void shouldRejectServiceResponseStatusException() throws Exception {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT);
    when(caseService.getLaunchURLForCaseId(any(), any())).thenThrow(ex);
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=12345"));
    actions
        .andExpect(status().isIAmATeapot())
        .andExpect(content().string(containsString("SYSTEM_ERROR")));
  }
}

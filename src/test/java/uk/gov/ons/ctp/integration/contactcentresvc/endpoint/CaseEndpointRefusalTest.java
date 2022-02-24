package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.SimpleDateFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/** Contact Centre Data Endpoint Unit tests */
@ExtendWith(MockitoExtension.class)
public final class CaseEndpointRefusalTest {

  private static final String CASE_ID = "caseId";
  private static final String REASON = "reason";
  private static final String NOTE = "note";
  private static final String ERASE_DATA = "eraseData";
  private static final String A_NOTE = "Respondent advises that they don't trust the ONS";

  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705Z";

  private CaseInteractionRequestDTO expectedInteractionDTO =
      CaseInteractionRequestDTO.builder().type(CaseInteractionType.REFUSAL_REQUESTED).build();

  @Mock private CaseService caseService;

  @Mock private InteractionService interactionService;

  @Mock RBACService rbacService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  @Captor private ArgumentCaptor<RefusalRequestDTO> requestCaptor;

  // UUID_STR must match the UUID in the test fixture
  private static final String UUID_STR = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

  @BeforeEach
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void refusalGoodRequest() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    assertOk(json);
  }

  @Test
  public void refusalGoodBodyCaseIdMismatch() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "c43533d0-7f2f-42d9-90d2-8204edf4812e");
    assertBadRequest(json);
  }

  @Test
  public void refusalForUnknownCaseFails() throws Exception {
    // Refusals no longer supports an 'unknown' case
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "unknown");
    ResultActions actions = mockMvc.perform(postJson("/cases/unknown/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void refusalBlankUUID() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    String uuid = "  ";
    json.put(CASE_ID, uuid);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/refusal", json.toString()));
    // pre 2.5 spring boot upgrade, this would return 400 result. Now the blank is converted to null
    // and fails later
    // message: "Required URI template variable 'caseId' for method parameter type UUID is present
    // but converted to null"
    actions.andExpect(status().is5xxServerError());
  }

  @Test
  public void refusalGoodBodyCaseIdNotUuid() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "fred");
    ResultActions actions = mockMvc.perform(postJson("/cases/fred/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void refusalReasonRequired() throws Exception {
    assertBadRequest(REASON, (String) null);
  }

  @Test
  public void refusalReasonBad() throws Exception {
    assertBadRequest(REASON, "NOT_A_REASON");
  }

  @Test
  public void shouldAcceptRefusalWithdrawal() throws Exception {
    assertOk(REASON, RefusalType.WITHDRAWAL_REFUSAL.name());
  }

  @Test
  public void refusalHardReasonOk() throws Exception {
    assertOk(REASON, RefusalType.HARD_REFUSAL.name());
  }

  @Test
  public void refusalExtraordinaryReasonOk() throws Exception {
    assertOk(REASON, RefusalType.EXTRAORDINARY_REFUSAL.name());
  }

  @Test
  public void refusalSoftReasonOk() throws Exception {
    assertOk(REASON, RefusalType.SOFT_REFUSAL.name());
  }

  @Test
  public void shouldRejectRefusalForEraseDataNull() throws Exception {
    assertBadRequest(ERASE_DATA, (String) null);
  }

  @Test
  public void shouldEraseDta() throws Exception {
    assertOk(ERASE_DATA, "true");
    assertTrue(requestCaptor.getValue().getEraseData());
  }

  @Test
  public void shouldNotEraseDta() throws Exception {
    assertOk(ERASE_DATA, "false");
    assertFalse(requestCaptor.getValue().getEraseData());
  }

  @Test
  public void shouldAcceptRefusalForNullNote() throws Exception {
    assertOk(NOTE, null);
    assertNull(requestCaptor.getValue().getNote());
  }

  @Test
  public void shouldHaveNote() throws Exception {
    assertOk(REASON, RefusalType.SOFT_REFUSAL.name());
    assertEquals(A_NOTE, requestCaptor.getValue().getNote());
  }

  private void assertOk(String field, String value) throws Exception {
    UUID uuid = UUID.randomUUID();
    SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    ResponseDTO responseDTO =
        ResponseDTO.builder()
            .id(uuid.toString())
            .dateTime(dateFormat.parse(RESPONSE_DATE_TIME))
            .build();
    when(caseService.reportRefusal(any(), any())).thenReturn(responseDTO);
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);

    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(uuid.toString())));
    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));

    verify(caseService).reportRefusal(any(), requestCaptor.capture());

    String refusalType = json.get(REASON).asText();
    String expectedNote = json.get(NOTE).isNull() ? null : json.get(NOTE).asText();
    expectedInteractionDTO.setSubtype(CaseSubInteractionType.valueOf("REFUSAL_" + refusalType));
    expectedInteractionDTO.setNote(expectedNote);

    verify(interactionService, times(1))
        .saveCaseInteraction(UUID.fromString(UUID_STR), expectedInteractionDTO);
  }

  private void assertBadRequest(String field, String value) throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  private void assertOk(ObjectNode json) throws Exception {
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isOk());

    expectedInteractionDTO.setSubtype(CaseSubInteractionType.valueOf("REFUSAL_HARD_REFUSAL"));
    expectedInteractionDTO.setNote(A_NOTE);

    verify(interactionService, times(1))
        .saveCaseInteraction(UUID.fromString(UUID_STR), expectedInteractionDTO);
  }

  private void assertBadRequest(ObjectNode json) throws Exception {
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Contact Centre Data Endpoint Unit tests */
@ExtendWith(MockitoExtension.class)
public final class CaseEndpointFulfilmentSMSTest {

  //  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705Z";
  //
  //  @Mock private CaseService caseService;
  //
  //  @Mock InteractionService interactionService;
  //
  //  @Mock RBACService rbacService;
  //
  //  @InjectMocks private CaseEndpoint caseEndpoint;
  //
  //  private MockMvc mockMvc;
  //
  //  private ObjectMapper mapper = new ObjectMapper();
  //
  //  /**
  //   * Set up of tests
  //   *
  //   * @throws Exception exception thrown
  //   */
  //  @BeforeEach
  //  public void setUp() throws Exception {
  //    this.mockMvc =
  //        MockMvcBuilders.standaloneSetup(caseEndpoint)
  //            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
  //            .setMessageConverters(new MappingJackson2HttpMessageConverter(new
  // CustomObjectMapper()))
  //            .build();
  //  }
  //
  //  @Test
  //  public void postFulfilmentByCaseById_GoodId() throws Exception {
  //    ObjectNode json = FixtureHelper.loadClassObjectNode();
  //    SMSFulfilmentRequestDTO requestData = mapper.convertValue(json,
  // SMSFulfilmentRequestDTO.class);
  //
  //    SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
  //    ResponseDTO responseDTO =
  //        ResponseDTO.builder()
  //            .id(requestData.getCaseId().toString())
  //            .dateTime(dateFormat.parse(RESPONSE_DATE_TIME))
  //            .build();
  //    Mockito.when(caseService.fulfilmentRequestBySMS(any())).thenReturn(responseDTO);
  //    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
  //    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
  //
  //    ResultActions actions =
  //        mockMvc.perform(
  //            postJson("/cases/" + requestData.getCaseId() + "/fulfilment/sms", json.toString()));
  //    actions.andExpect(status().isOk());
  //    actions.andExpect(jsonPath("$.id", is(requestData.getCaseId().toString())));
  //    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));
  //
  //    CaseInteractionRequestDTO interactionDTO =
  //        CaseInteractionRequestDTO.builder()
  //            .type(CaseInteractionType.FULFILMENT_REQUESTED)
  //            .subtype(CaseSubInteractionType.FULFILMENT_SMS)
  //            .note(requestData.getFulfilmentCode())
  //            .build();
  //    verify(interactionService, times(1))
  //        .saveCaseInteraction(requestData.getCaseId(), interactionDTO);
  //  }
  //
  //  @Test
  //  public void postFulfilmentByCaseById_BadId() throws Exception {
  //    ObjectNode json = FixtureHelper.loadClassObjectNode();
  //    ResultActions actions =
  //        mockMvc.perform(postJson("/cases/123456789/fulfilment/sms", json.toString()));
  //    actions.andExpect(status().isBadRequest());
  //  }
  //
  //  @Test
  //  public void postFulfilmentByCaseById_BadDate() throws Exception {
  //    assertBadRequest("dateTime", "2019:12:25 12:34:56");
  //  }
  //
  //  @Test
  //  public void postFulfilmentByCaseById_BadCaseId() throws Exception {
  //    assertBadRequest("caseId", "foo");
  //  }
  //
  //  @Test
  //  public void postFulfilmentByCaseById_BadTelNo() throws Exception {
  //    assertBadRequest("telNo", "ABC123");
  //  }
  //
  //  private void assertBadRequest(String field, String value) throws Exception {
  //    ObjectNode json = FixtureHelper.loadClassObjectNode();
  //    SMSFulfilmentRequestDTO requestData = mapper.convertValue(json,
  // SMSFulfilmentRequestDTO.class);
  //    json.put(field, value);
  //    ResultActions actions =
  //        mockMvc.perform(
  //            postJson("/cases/" + requestData.getCaseId() + "/fulfilment/sms", json.toString()));
  //    actions.andExpect(status().isBadRequest());
  //  }
}

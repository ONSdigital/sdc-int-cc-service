package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.InteractionService;

/**
 * Contact Centre Data Endpoint Unit tests. This class tests the get case endpoints, covering gets
 * based on uuid, ref and the sample attribute search.
 */
@ExtendWith(MockitoExtension.class)
public final class CaseEndpointGetCaseTest {

  private static final String CASE_UUID_STRING = "dca05c61-8b95-46af-8f73-36f0dc2cbf5e";
  private static final String CASE_REF = "123456";
  private static final String ADDRESS_LINE_1 = "Smiths Renovations";
  private static final String ADDRESS_LINE_2 = "Rock House";
  private static final String ADDRESS_LINE_3 = "Cowick Lane";
  private static final String TOWN = "Exeter";
  private static final Region REGION = Region.E;
  private static final String POSTCODE = "EX2 9HY";

  private static final String EVENT_CATEGORY = "REFUSAL";
  private static final String EVENT_DESCRIPTION = "Event for testcase";
  private static final String EVENT_DATE_TIME = "2017-02-11T16:32:11.863Z";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  @Mock InteractionService interactionService;

  private MockMvc mockMvc;

  private UUID uuid = UUID.randomUUID();

  private CaseInteractionDTO interactionDTO =
      CaseInteractionDTO.builder().type(CaseInteractionType.MANUAL_CASE_VIEW).build();

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
  public void getCaseById_GoodId() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseById(eq(uuid), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);

    verify(interactionService, times(1)).saveCaseInteraction(uuid, interactionDTO);
  }

  @Test
  public void getCaseById_BadId() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/123456789"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseById_CaseEventsTrue() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseById(eq(uuid), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=1"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);

    verify(interactionService, times(1)).saveCaseInteraction(uuid, interactionDTO);
  }

  @Test
  public void getCaseById_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseByRef_GoodRef() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseByCaseReference(eq(123456L), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/ref/123456"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);

    verify(interactionService, times(1))
        .saveCaseInteraction(UUID.fromString(CASE_UUID_STRING), interactionDTO);
  }

  @Test
  public void getCaseByRef_BadRef() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/ref/avg"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseBySampleAttribute_GoodUPRN() throws Exception {
    List<CaseDTO> testCases = new ArrayList<>();
    testCases.add(createResponseCaseDTO());
    testCases.add(createResponseCaseDTO());
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(
            caseService.getCaseBySampleAttribute(
                eq(CaseUpdate.ATTRIBUTE_UPRN), eq(String.valueOf(expectedUprn.getValue())), any()))
        .thenReturn(testCases);

    ResultActions actions = mockMvc.perform(getJson("/cases/attribute/uprn/123456789012"));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  @Test
  public void getCaseBySampleAttribute_CaseEventsTrue() throws Exception {
    List<CaseDTO> testCases = new ArrayList<>();
    testCases.add(createResponseCaseDTO());
    testCases.add(createResponseCaseDTO());
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(
            caseService.getCaseBySampleAttribute(
                eq(CaseUpdate.ATTRIBUTE_UPRN), eq(String.valueOf(expectedUprn.getValue())), any()))
        .thenReturn(testCases);

    ResultActions actions =
        mockMvc.perform(getJson("/cases/attribute/uprn/123456789012?caseEvents=1"));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  @Test
  public void getCaseBySampleAttribute_CaseEventsDuff() throws Exception {
    ResultActions actions =
        mockMvc.perform(getJson("/cases/attribute/uprn/12345678901234?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }

  private CaseDTO createResponseCaseDTO() throws ParseException {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .description(EVENT_DESCRIPTION)
            .category(EVENT_CATEGORY)
            .createdDateTime(formatter.parse(EVENT_DATE_TIME))
            .build();

    Map<String, Object> fakeSample = new HashMap<>();
    fakeSample.put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_1, ADDRESS_LINE_1);
    fakeSample.put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_2, ADDRESS_LINE_2);
    fakeSample.put(CaseUpdate.ATTRIBUTE_ADDRESS_LINE_3, ADDRESS_LINE_3);
    fakeSample.put(CaseUpdate.ATTRIBUTE_TOWN_NAME, TOWN);
    fakeSample.put(CaseUpdate.ATTRIBUTE_POSTCODE, POSTCODE);
    fakeSample.put(CaseUpdate.ATTRIBUTE_REGION, REGION.name());

    return CaseDTO.builder()
        .id(UUID.fromString(CASE_UUID_STRING))
        .caseRef(CASE_REF)
        .sample(fakeSample)
        .caseEvents(Arrays.asList(caseEventDTO1))
        .build();
  }

  private void verifyStructureOfResultsActions(ResultActions actions) throws Exception {
    actions.andExpect(jsonPath("$.id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$.caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$.sample.addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$.sample.addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$.sample.addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$.sample.townName", is(TOWN)));
    actions.andExpect(jsonPath("$.sample.region", is(REGION.name())));
    actions.andExpect(jsonPath("$.sample.postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$.caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$.caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$.caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }

  private void verifyStructureOfMultiResultsActions(ResultActions actions) throws Exception {
    // This is not ideal - obvious duplication here - want to find a neater way of making the same
    // assertions repeatedly
    actions.andExpect(jsonPath("$[0].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[0].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[0].sample.addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$[0].sample.addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$[0].sample.addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$[0].sample.townName", is(TOWN)));
    actions.andExpect(jsonPath("$[0].sample.region", is(REGION.name())));
    actions.andExpect(jsonPath("$[0].sample.postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$[0].caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$[0].caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$[0].caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));

    actions.andExpect(jsonPath("$[1].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[1].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[1].sample.addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$[1].sample.addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$[1].sample.addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$[1].sample.townName", is(TOWN)));
    actions.andExpect(jsonPath("$[1].sample.region", is(REGION.name())));
    actions.andExpect(jsonPath("$[1].sample.postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$[1].caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }
}

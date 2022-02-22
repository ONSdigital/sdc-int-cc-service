package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseSummaryDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/**
 * Contact Centre Data Endpoint Unit tests. This class tests the get case endpoints, covering gets
 * based on uuid, ref and the sample attribute search.
 */
@ExtendWith(MockitoExtension.class)
public final class CaseEndpoint_getCaseSummaryBySampleAttribute {

  private static final String CASE_UUID_STRING = "dca05c61-8b95-46af-8f73-36f0dc2cbf5e";
  private static final String CASE_REF = "123456";
  private static final String SURVEY_NAME = "Hat size survey";
  private static final String SURVEY_TYPE = "cis";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  @Mock InteractionService interactionService;

  @Mock RBACService rbacService;

  private MockMvc mockMvc;

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
  public void getCaseById_BadId() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/123456789"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseSummaryBySampleAttribute_GoodUPRN() throws Exception {
    List<CaseSummaryDTO> caseSummaries = new ArrayList<>();
    caseSummaries.add(createCaseSummaryDTO());
    caseSummaries.add(createCaseSummaryDTO());
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(
            caseService.getCaseSummaryBySampleAttribute(
                eq(CaseUpdate.ATTRIBUTE_UPRN), eq(String.valueOf(expectedUprn.getValue()))))
        .thenReturn(caseSummaries);

    ResultActions actions = mockMvc.perform(getJson("/cases/attribute/uprn/123456789012"));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  private CaseSummaryDTO createCaseSummaryDTO() throws ParseException {
    return CaseSummaryDTO.builder()
        .id(UUID.fromString(CASE_UUID_STRING))
        .caseRef(CASE_REF)
        .surveyName(SURVEY_NAME)
        .surveyType(SURVEY_TYPE)
        .build();
  }

  private void verifyStructureOfMultiResultsActions(ResultActions actions) throws Exception {
    // This is not ideal - obvious duplication here - want to find a neater way of making the same
    // assertions repeatedly
    actions.andExpect(jsonPath("$[0].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[0].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[0].surveyName", is(SURVEY_NAME)));
    actions.andExpect(jsonPath("$[0].surveyType", is(SURVEY_TYPE)));

    actions.andExpect(jsonPath("$[1].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[1].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[1].surveyName", is(SURVEY_NAME)));
    actions.andExpect(jsonPath("$[1].surveyType", is(SURVEY_TYPE)));
  }
}

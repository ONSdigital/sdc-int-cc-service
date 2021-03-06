package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.contactcentresvc.config.Fulfilments;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

/*
 * PMB
 * import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.config.Fulfilments;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

 */

/**
 * Unit Test {@link CaseService#fulfilmentRequestByPost(PostalFulfilmentRequestDTO)
 * fulfilmentRequestByPost} and {@link CaseService#fulfilmentRequestBySMS(SMSFulfilmentRequestDTO)
 * fulfilmentRequestBySMS}
 */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplFulfilmentTest extends CaseServiceImplTestBase {

  private static final String BLACK_LISTED_PACK_CODE = "P_TB_TBBEN1";

  @Mock private InteractionService interactionService;

  @BeforeEach
  public void setup() {
    Mockito.lenient().when(appConfig.getSurveyName()).thenReturn("CENSUS");

    Fulfilments fulfilments = new Fulfilments();
    fulfilments.setBlacklistedCodes(Set.of(BLACK_LISTED_PACK_CODE));
    Mockito.when(appConfig.getFulfilments()).thenReturn(fulfilments);
  }

  private static Stream<Arguments> dataForFulfilmentPostSuccess() {
    return Stream.of(
        arguments("Bill", "Bloggs"),
        arguments("Tom", null),
        arguments(null, "Smith"),
        arguments(null, null),
        arguments("", ""));
  }

  @ParameterizedTest
  @MethodSource("dataForFulfilmentPostSuccess")
  public void fulfilmentRequestByPostShouldSucceed(String firstName, String lastName)
      throws Exception {
    Mockito.clearInvocations(eventTransfer);

    Case caze = casesFromDb().get(0);

    mockGetCaseById(CASE_ID_0, caze);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(CASE_ID_0, firstName, lastName);

    // execution - call the unit under test
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO responseDTOFixture = target.fulfilmentRequestByPost(requestBodyDTOFixture);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), responseDTOFixture.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, responseDTOFixture.getDateTime());

    // Grab the published event
    FulfilmentRequest actualFulfilmentRequest =
        verifyEventSent(TopicType.FULFILMENT, FulfilmentRequest.class);
    assertEquals(requestBodyDTOFixture.getPackCode(), actualFulfilmentRequest.getPackCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    // Verify first/last name in the published event
    assertEquals(CASE_ID_0.toString(), actualFulfilmentRequest.getCaseId());
    Map<String, Object> personalisation = actualFulfilmentRequest.getPersonalisation();
    if (Strings.isBlank(firstName)) {
      assertFalse(personalisation.containsKey("firstName"));
    } else {
      assertEquals(firstName, personalisation.get("firstName"));
    }
    if (Strings.isBlank(lastName)) {
      assertFalse(personalisation.containsKey("lastName"));
    } else {
      assertEquals(lastName, personalisation.get("lastName"));
    }
  }

  @Test
  public void testFulfilmentRequestByPost_blackListedFulfilmentCode() throws Exception {
    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(CASE_ID_0, "Mickey", "Mouse");
    requestBodyDTOFixture.setPackCode(BLACK_LISTED_PACK_CODE);

    try {
      target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage().contains("is no longer available"), e.getMessage());
      assertEquals(Fault.BAD_REQUEST, e.getFault());
    }
  }

  @Test
  public void testFulfilmentRequestByPost_caseSvcNotFoundResponse() throws Exception {
    mockGetCaseById(CASE_ID_0, new CTPException(Fault.RESOURCE_NOT_FOUND));
    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(CASE_ID_0, "Sally", "Smurf");
    assertThrows(CTPException.class, () -> target.fulfilmentRequestByPost(requestBodyDTOFixture));
  }

  @Test
  public void testFulfilmentRequestByPost_caseSvcRestClientException() throws Exception {
    mockGetCaseById(CASE_ID_0, new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT));

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(CASE_ID_0, "Sally", "Smurf");

    assertThrows(
        ResponseStatusException.class, () -> target.fulfilmentRequestByPost(requestBodyDTOFixture));
  }

  //  @Test
  //  public void testFulfilmentRequestBySMSSuccess_withCaseTypeHH() throws Exception {
  //    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, false);
  //  }
  //
  //  @Test
  //  public void testFulfilmentRequestBySMSSuccess_withIndividualTrue() throws Exception {
  //    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, true);
  //  }
  //
  //  @Test
  //  public void testFulfilmentRequestBySMS_blackListedFulfilmentCode() throws Exception {
  //    Case caseData = casesFromDb().get(0);
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);
  //    requestBodyDTOFixture.setFulfilmentCode(BLACK_LISTED_FULFILMENT_CODE);
  //
  //    try {
  //      target.fulfilmentRequestBySMS(requestBodyDTOFixture);
  //      fail();
  //    } catch (CTPException e) {
  //      assertTrue(e.getMessage().contains("is no longer available"), e.getMessage());
  //      assertEquals(Fault.BAD_REQUEST, e.getFault());
  //    }
  //  }
  //
  //  @Test
  //  public void testFulfilmentRequestBySMSFailure_productNotFound() throws Exception {
  //
  //    // Build results to be returned from search
  //    Case caze = casesFromDb().get(0);
  //    mockGetCaseById(CASE_ID_0, caze);
  //
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caze);
  //
  //    Product expectedSearchCriteria =
  //        getExpectedSearchCriteria(
  //            Product.Region.E,
  //            requestBodyDTOFixture.getFulfilmentCode(),
  //            Product.DeliveryChannel.SMS);
  //
  //    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
  //        .thenReturn(new ArrayList<Product>());
  //
  //    try {
  //      // execution - call the unit under test
  //      target.fulfilmentRequestBySMS(requestBodyDTOFixture);
  //      fail();
  //    } catch (CTPException e) {
  //      assertEquals("Compatible product cannot be found", e.getMessage());
  //      assertEquals("BAD_REQUEST", e.getFault().name());
  //    }
  //  }
  //
  //  @Test
  //  public void testFulfilmentRequestBySMS_caseSvcNotFoundResponse() throws Exception {
  //    Case caseData = casesFromDb().get(0);
  //    mockGetCaseById(CASE_ID_0, new CTPException(Fault.RESOURCE_NOT_FOUND));
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);
  //    assertThrows(CTPException.class, () ->
  // target.fulfilmentRequestBySMS(requestBodyDTOFixture));
  //  }
  //
  //  @Test
  //  public void testFulfilmentRequestBySMS_caseSvcRestClientException() throws Exception {
  //    Case caseData = casesFromDb().get(0);
  //    mockGetCaseById(CASE_ID_0, new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT));
  //
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);
  //
  //    assertThrows(
  //        ResponseStatusException.class, () ->
  // target.fulfilmentRequestBySMS(requestBodyDTOFixture));
  //  }

  private PostalFulfilmentRequestDTO getPostalFulfilmentRequestDTO(
      UUID caseId, String firstName, String lastName) {
    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();
    requestBodyDTOFixture.setCaseId(caseId);
    requestBodyDTOFixture.setPackCode("ABC123");
    requestBodyDTOFixture.setFirstName(firstName);
    requestBodyDTOFixture.setLastName(lastName);
    return requestBodyDTOFixture;
  }

  //  private SMSFulfilmentRequestDTO getSMSFulfilmentRequestDTO(Case caze) {
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture = new SMSFulfilmentRequestDTO();
  //    requestBodyDTOFixture.setCaseId(caze.getId());
  //    requestBodyDTOFixture.setTelNo("+447890000000");
  //    requestBodyDTOFixture.setFulfilmentCode("ABC123");
  //    requestBodyDTOFixture.setDateTime(DateTimeUtil.nowUTC());
  //    return requestBodyDTOFixture;
  //  }
  //
  //  private Product getProductFoundFixture(
  //      List<Product.CaseType> caseTypes,
  //      Product.DeliveryChannel deliveryChannel,
  //      boolean individual) {
  //    return Product.builder()
  //        .caseTypes(caseTypes)
  //        .description("foobar")
  //        .fulfilmentCode("ABC123")
  //        .deliveryChannel(deliveryChannel)
  //        .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
  //        .requestChannels(
  //            new ArrayList<Product.RequestChannel>(
  //                List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
  //        .individual(individual)
  //        .build();
  //  }
  //
  //  private Product getExpectedSearchCriteria(
  //      Product.Region region, String fulfilmentCode, Product.DeliveryChannel deliveryChannel) {
  //    return Product.builder()
  //        .fulfilmentCode(fulfilmentCode)
  //        .requestChannels(Arrays.asList(Product.RequestChannel.CC))
  //        .deliveryChannel(deliveryChannel)
  //        .regions(Arrays.asList(region))
  //        .build();
  //  }
  //
  //  private void doFulfilmentRequestBySMSSuccess(Product.CaseType caseType, boolean individual)
  //      throws Exception {
  //
  //    Case caseFromCaseService = casesFromDb().get(0);
  //    mockGetCaseById(CASE_ID_0, caseFromCaseService);
  //
  //    SMSFulfilmentRequestDTO requestBodyDTOFixture =
  // getSMSFulfilmentRequestDTO(caseFromCaseService);
  //
  //    Product expectedSearchCriteria =
  //        getExpectedSearchCriteria(
  //            Product.Region.E,
  //            requestBodyDTOFixture.getFulfilmentCode(),
  //            Product.DeliveryChannel.SMS);
  //
  //    // The mocked productReference will return this product
  //    Product productFoundFixture =
  //        getProductFoundFixture(Arrays.asList(caseType), Product.DeliveryChannel.SMS,
  // individual);
  //    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
  //        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));
  //
  //    // execution - call the unit under test
  //    long timeBeforeInvocation = System.currentTimeMillis();
  //    ResponseDTO response = target.fulfilmentRequestBySMS(requestBodyDTOFixture);
  //    long timeAfterInvocation = System.currentTimeMillis();
  //
  //    // Validate the response
  //    assertEquals(requestBodyDTOFixture.getCaseId().toString(), response.getId());
  //    verifyTimeInExpectedRange(timeBeforeInvocation, timeAfterInvocation,
  // response.getDateTime());
  //
  //    // Grab the published event
  //    FulfilmentRequest actualFulfilmentRequest =
  //        verifyEventSent(TopicType.FULFILMENT, FulfilmentRequest.class);
  //    assertEquals(
  //        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
  //    assertEquals(requestBodyDTOFixture.getCaseId().toString(),
  // actualFulfilmentRequest.getCaseId());
  //
  //    if (caseType == Product.CaseType.HH && individual) {
  //      assertNotNull(actualFulfilmentRequest.getIndividualCaseId());
  //    } else {
  //      assertNull(actualFulfilmentRequest.getIndividualCaseId());
  //    }
  //
  //    Contact actualContact = actualFulfilmentRequest.getContact();
  //    assertNull(actualContact.getTitle());
  //    assertNull(actualContact.getForename());
  //    assertNull(actualContact.getSurname());
  //    assertEquals(requestBodyDTOFixture.getTelNo(), actualContact.getTelNo());
  //  }

  private List<Case> casesFromDb() {
    return FixtureHelper.loadPackageFixtures(Case[].class);
  }
}

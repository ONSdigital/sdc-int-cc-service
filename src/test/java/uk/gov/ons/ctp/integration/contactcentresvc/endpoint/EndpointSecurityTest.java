package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_1;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_2;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_3;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ESTAB_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_CASE_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REQUEST_DATE_TIME;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.CASE_ID_0;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendPoller;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/**
 * This test class started off during Census when we restricted access to basic auth serco AND the
 * CC service had a dual mode CC/AD persona which meant that endpoints would/would not be accessible
 * depending on the mode in hand, or if they did not auth correctlt. I have gone to the trouble of
 * updating many of the tests here so that they work with the RBAC identity aware endpoints and
 * their permission checks, but looking through these tests now they currently little or no value -
 * all they are doing is exercising each endpoint and asserting an ok response. The framework in
 * this class could be useful for building out some more extensive tests to test the RBAC checks in
 * each endpoint so leaving here as is for time being
 *
 * @author philwhiles
 */
@ActiveProfiles("test-cc")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean({EventToSendPoller.class, EventPublisher.class, PubSubTemplate.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class EndpointSecurityTest {

  @MockBean CaseService caseService;
  @MockBean InteractionService interactionService;
  @MockBean AddressService addressService;
  @MockBean RBACService rbacService;

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setUp() throws MalformedURLException {
    restTemplate = new TestRestTemplate(new RestTemplateBuilder());
    base = new URL("http://localhost:" + port);
    when(rbacService.userActingAsAllowedDummy()).thenReturn(true);
  }

  @Test
  public void whenLoggedUserRequestsInfoPageThenSuccess() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("ccsvc"));
  }

  @Test
  public void whenAnonymousUserRequestsInfoPageThenSuccess() {
    restTemplate = new TestRestTemplate();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("ccsvc"), response.getBody());
  }

  @Test
  public void testOkAccessCasesByUPRN() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/ccsvc/cases/attribute/uprn/123456789012", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetAddresses() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/ccsvc/addresses?input=2A%20Priors%20Way", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseByUPRN() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/ccsvc/addresses/postcode?postcode=EX10 1BD", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPostRefusal() throws CTPException {
    UUID caseId = UUID.randomUUID();
    RefusalRequestDTO requestBody = new RefusalRequestDTO();
    requestBody.setCaseId(caseId);
    requestBody.setReason(RefusalType.HARD_REFUSAL);
    requestBody.setDateTime(new Date());
    mockGetSurvey();

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/refusal", requestBody, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPutCase() throws CTPException {
    ModifyCaseRequestDTO requestBody = ModifyCaseRequestDTO.builder().caseId(CASE_ID_0).build();

    requestBody.setAddressLine1(AN_ADDRESS_LINE_1);
    requestBody.setAddressLine2(AN_ADDRESS_LINE_2);
    requestBody.setAddressLine3(AN_ADDRESS_LINE_3);
    requestBody.setDateTime(A_REQUEST_DATE_TIME);
    requestBody.setCaseType(A_CASE_TYPE);
    requestBody.setEstabType(AN_ESTAB_TYPE);
    mockGetSurvey();

    HttpHeaders headers = new HttpHeaders();
    Map<String, String> param = new HashMap<String, String>();
    HttpEntity<ModifyCaseRequestDTO> requestEntity =
        new HttpEntity<ModifyCaseRequestDTO>(requestBody, headers);
    ResponseEntity<ResponseDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/cases/" + requestBody.getCaseId(),
            HttpMethod.PUT,
            requestEntity,
            ResponseDTO.class,
            param);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseByCaseId() throws CTPException {
    UUID caseId = UUID.randomUUID();
    mockGetSurvey();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/cases/" + caseId, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseByCaseRef() throws CTPException {
    mockGetSurvey();
    CaseDTO caze = CaseDTO.builder().id(CASE_ID_0).build();
    when(caseService.getCaseByCaseReference(eq(123456789L), any())).thenReturn(caze);
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/cases/ref/123456789", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseLaunch() throws CTPException {
    UUID caseId = UUID.randomUUID();
    mockGetSurvey();
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/launch?individual=false&agentId=12345",
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetFulfilfments() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/fulfilments", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPostFulfilmentPost() throws CTPException {
    UUID caseId = UUID.randomUUID();
    PostalFulfilmentRequestDTO requestBody = new PostalFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");
    mockGetSurvey();

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/fulfilment/post",
            requestBody,
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPostFulfilmentSMS() throws CTPException {
    UUID caseId = UUID.randomUUID();
    SMSFulfilmentRequestDTO requestBody = new SMSFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");
    requestBody.setTelNo("447123456789");
    mockGetSurvey();

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/fulfilment/sms",
            requestBody,
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private void mockGetSurvey() throws CTPException {
    SurveyDTO surveyDTO = SurveyDTO.builder().id(UUID.randomUUID()).build();
    when(caseService.getSurveyForCase(any())).thenReturn(surveyDTO);
  }
}

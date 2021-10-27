package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_1;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_2;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_3;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ESTAB_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_CASE_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REQUEST_DATE_TIME;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class EndpointSecurityTest {
  @MockBean CaseService caseService;
  @MockBean AddressService addressService;

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setUp() throws MalformedURLException {
    restTemplate = new TestRestTemplate("serco_cks", "temporary");
    base = new URL("http://localhost:" + port);
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
  public void whenUserWithWrongCredentialsRequestsVersionThenUnauthorizedPage() throws Exception {

    restTemplate = new TestRestTemplate("user", "wrongpassword");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/version", String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  public void whenUserWithCorrectCredentialsRequestsVersionThenSuccess() throws Exception {

    restTemplate = new TestRestTemplate("serco_cks", "temporary");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/version", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkAccessCasesByUPRN() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/cases/uprn/123456789012", String.class);

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
  public void testOkPostRefusal() {
    UUID caseId = UUID.randomUUID();
    RefusalRequestDTO requestBody = new RefusalRequestDTO();
    requestBody.setCaseId(caseId);
    requestBody.setReason(Reason.HARD);
    requestBody.setAgentId(12345);
    requestBody.setCallId("8989-NOW");
    requestBody.setIsHouseholder(false);
    requestBody.setDateTime(new Date());

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/refusal", requestBody, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPutCase() {
    ModifyCaseRequestDTO requestBody = ModifyCaseRequestDTO.builder().caseId(UUID_0).build();

    requestBody.setAddressLine1(AN_ADDRESS_LINE_1);
    requestBody.setAddressLine2(AN_ADDRESS_LINE_2);
    requestBody.setAddressLine3(AN_ADDRESS_LINE_3);
    requestBody.setDateTime(A_REQUEST_DATE_TIME);
    requestBody.setCaseType(A_CASE_TYPE);
    requestBody.setEstabType(AN_ESTAB_TYPE);

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
  public void testOkGetCaseByCaseId() {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/cases/" + caseId, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseByCaseRef() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/cases/ref/123456789", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkGetCaseLaunch() {
    UUID caseId = UUID.randomUUID();
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
  public void testOkPostFulfilmentPost() {
    UUID caseId = UUID.randomUUID();
    PostalFulfilmentRequestDTO requestBody = new PostalFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/fulfilment/post",
            requestBody,
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void testOkPostFulfilmentSMS() {
    UUID caseId = UUID.randomUUID();
    SMSFulfilmentRequestDTO requestBody = new SMSFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");
    requestBody.setTelNo("447123456789");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/ccsvc/cases/" + caseId + "/fulfilment/sms",
            requestBody,
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

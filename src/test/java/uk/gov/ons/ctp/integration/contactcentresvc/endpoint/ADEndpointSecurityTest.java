package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.net.URL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@Disabled("FIXME")
@ActiveProfiles("test-ad")
public class ADEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;
  @MockBean AddressService addressService;

  @Test
  public void adOkGetUACForCase() {
    testGetUACForCase(HttpStatus.OK);
  }

  @Test
  public void adOkAccessCasesByUPRN() {
    testAccessCasesByUPRN(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddresses() {
    testGetAddresses(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddressesPostcode() {
    testGetAddressesPostcode(HttpStatus.OK);
  }

  @Test
  public void adForbiddenPostRefusal() {
    testPostRefusal(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPutCase() {
    testPutCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCCSCaseByPostcode() {
    testGetCCSCaseByPostcode(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseId() {
    testGetCaseByCaseId(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseRef() {
    testGetCaseByCaseRef(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseLaunch() {
    testGetCaseLaunch(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetFulfilfments() {
    testGetFulfilfments(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentPost() {
    testPostFulfilmentPost(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentSMS() {
    testPostFulfilmentSMS(HttpStatus.FORBIDDEN);
  }
}

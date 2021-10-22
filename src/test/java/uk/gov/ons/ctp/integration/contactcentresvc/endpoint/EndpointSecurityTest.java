package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"GOOGLE_CLOUD_PROJECT=census-cc-test"})
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public abstract class EndpointSecurityTest {

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
  public void whenUserWithCorrectCredentialsRequestsVersionThenSuccess() throws Exception {

    restTemplate = new TestRestTemplate("serco_cks", "temporary");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/ccsvc/version", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

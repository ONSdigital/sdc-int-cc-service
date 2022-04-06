package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

/** Integration test for user login. */
public class UserLoginIT extends FullStackIntegrationTestBase {

  private static UUID FRED_UUID = UUID.fromString("4f27ee97-7ba7-4979-b9e8-bfe3063b41e8");

  @Autowired private TransactionalOps txOps;

  @Autowired private UserRepository userRepo;

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setup() throws MalformedURLException {
    base = new URL("http://localhost:" + port);

    restTemplate = new TestRestTemplate(new RestTemplateBuilder());

    txOps.deleteAll();
  }

  @Test
  public void login() throws Exception {

    txOps.createUser("Fred@ext.ons.gov.uk", FRED_UUID);

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "Fred@ext.ons.gov.uk");

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename("Hamish");
    loginRequestDTO.setSurname("Wilson");

    HttpEntity<LoginRequestDTO> requestEntity =
        new HttpEntity<LoginRequestDTO>(loginRequestDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/login",
            HttpMethod.PUT,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify that the response uses the updated name
    UserDTO responseDTO = response.getBody();
    assertEquals("Hamish", responseDTO.getForename());
    assertEquals("Wilson", responseDTO.getSurname());
    assertTrue(responseDTO.isActive());

    // Verify that DB contains the new name
    User userInDB = userRepo.findById(FRED_UUID).get();
    assertEquals("Hamish", userInDB.getForename());
    assertEquals("Wilson", userInDB.getSurname());
    assertTrue(userInDB.isActive());
  }

  @Test
  public void loginDeletedUser() throws Exception {

    txOps.createDeletedUser("Fred@ext.ons.gov.uk", FRED_UUID);

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "Fred@ext.ons.gov.uk");

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename("Hamish");
    loginRequestDTO.setSurname("Wilson");

    HttpEntity<LoginRequestDTO> requestEntity =
        new HttpEntity<LoginRequestDTO>(loginRequestDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/login",
            HttpMethod.PUT,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }
}

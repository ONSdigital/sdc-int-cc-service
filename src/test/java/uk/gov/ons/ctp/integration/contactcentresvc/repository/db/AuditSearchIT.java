package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

/** Integration test for user login. */
public class AuditSearchIT extends FullStackIntegrationTestBase {

  private static UUID BOSS_MAN_UUID = UUID.fromString("3f37922d-163c-4557-95e6-06b09637e9ee");
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

  //  txOps.createUser("BossMan@ext.ons.gov.uk", BOSS_MAN_UUID);
//	  txOps.createUser("Fred@ext.ons.gov.uk", FRED_UUID);

	  callCreateUserEndpoint("BossMan@ext.ons.gov.uk");
	  
    callLoginEndpoint("BossMan@ext.ons.gov.uk");
    callLoginEndpoint("Fred@ext.ons.gov.uk");

    callAuditEndpoint("BossMan@ext.ons.gov.uk", "BossMan@ext.ons.gov.uk", null);
  }

	private void callCreateUserEndpoint(String userIdentity) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "philip.whiles@ext.ons.gov.uk");

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);
    
    HttpEntity<UserDTO> requestEntity = new HttpEntity<UserDTO>(userDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users",
            HttpMethod.POST,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private void callAuditEndpoint(String userIdentity, String principle, String targetUser) {

	  HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);

    Map<String, String> params = new HashMap<String, String>();
    params.put("principle", principle);
    params.put("targetUser", targetUser);

    ResponseEntity<List<UserAuditDTO>> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/audit?principle={principle}&targetUser={targetUser}",
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<UserAuditDTO>>() {},
            params);
    
    List<UserAuditDTO> body = response.getBody();
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private void callLoginEndpoint(String userIdentity) {
		HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

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
	}

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;

    public TransactionalOps(UserRepository userRepo) {
      this.userRepo = userRepo;
    }

    public void deleteAll() {
      userRepo.deleteAll();
    }

    public void createUser(String name, UUID id) {
      createUser(name, id, null, null);
    }

    public void createUser(String name, UUID id, List<Role> userRoles, List<Role> adminRoles) {
      User user =
          User.builder()
              .id(id)
              .identity(name)
              .userRoles(userRoles == null ? Collections.emptyList() : userRoles)
              .adminRoles(adminRoles == null ? Collections.emptyList() : adminRoles)
              .build();
      userRepo.save(user);
    }
  }
}

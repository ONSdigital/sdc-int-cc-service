package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

public class UserServiceIT extends FullStackIntegrationTestBase {

  private static final UUID USER_MANAGER = UUID.fromString("4f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID DELETE_TARGET = UUID.fromString("5f27ee97-7ba7-4979-b9e8-bfe3063b41e8");

  @Autowired private UserServiceIT.TransactionalOps txOps;

  @Autowired private UserRepository userRepo;

  @Autowired private RoleRepository roleRepository;

  TestRestTemplate restTemplate;

  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setup() throws MalformedURLException {
    base = new URL("http://localhost:" + port);

    restTemplate = new TestRestTemplate(new RestTemplateBuilder());

    Role role = roleRepository.findByName("Super User").get();

    txOps.deleteAll();
    txOps.createUser("user.manager@ext.ons.gov.uk", USER_MANAGER, List.of(role), null);
    txOps.createUser("delete.target@ext.ons.gov.uk", DELETE_TARGET);
  }

  @Test
  public void deleteUser() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit delete request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/delete.target@ext.ons.gov.uk",
            HttpMethod.DELETE,
            requestEntity,
            UserDTO.class,
            params);

    User user = userRepo.findByIdentity("delete.target@ext.ons.gov.uk").get();

    assertTrue(user.isDeleted());
  }

  @Test
  public void deleteUserWhoIsUnDeletable() {
    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.set("x-user-id", "delete.target@ext.ons.gov.uk");

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename("delete");
    loginRequestDTO.setSurname("target");

    HttpEntity<LoginRequestDTO> loginRequestEntity =
        new HttpEntity<LoginRequestDTO>(loginRequestDTO, loginHeaders);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> loginResponse =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/login",
            HttpMethod.PUT,
            loginRequestEntity,
            UserDTO.class,
            params);

    assertFalse(loginResponse.getBody().isDeletable());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    // Submit delete request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/delete.target@ext.ons.gov.uk",
            HttpMethod.DELETE,
            requestEntity,
            UserDTO.class,
            params);

    User user = userRepo.findByIdentity("delete.target@ext.ons.gov.uk").get();

    assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    assertFalse(user.isDeleted());
  }

  @Test
  public void deleteUserSelfDeleteBlocked() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit delete request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/user.manager@ext.ons.gov.uk",
            HttpMethod.DELETE,
            requestEntity,
            UserDTO.class,
            params);

    User user = userRepo.findByIdentity("user.manager@ext.ons.gov.uk").get();

    assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    assertFalse(user.isDeleted());
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;
    private UserAuditRepository auditRepository;

    public TransactionalOps(UserRepository userRepo, UserAuditRepository auditRepository) {
      this.userRepo = userRepo;
      this.auditRepository = auditRepository;
    }

    public void deleteAll() {
      auditRepository.deleteAll();
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

package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
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
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

public class UserEndpointIT extends FullStackIntegrationTestBase {

  private static final UUID DELETER_ROLE_ID =
      UUID.fromString("7121e404-9ec0-11ec-a16c-4c3275913db5");
  private final String PHIL = "philip.whiles@ext.ons.gov.uk";

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

    var perms =
        List.of(
            PermissionType.DELETE_USER,
            PermissionType.READ_USER,
            PermissionType.USER_SURVEY_MAINTENANCE,
            PermissionType.USER_ROLE_MAINTENANCE,
            PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE);
    Role role = txOps.createRole("deleter", DELETER_ROLE_ID, perms);
    txOps.createUser("user.manager@ext.ons.gov.uk", UUID.randomUUID(), List.of(role), null);
    txOps.createUser("delete.target@ext.ons.gov.uk", UUID.randomUUID(), List.of(role), null);
  }

  @Test
  public void shouldCreateUser() throws Exception {
    Role role =
        txOps.createRole("user-creator", UUID.randomUUID(), List.of(PermissionType.CREATE_USER));
    txOps.createUser("alison.pritchard@ons.gov.uk", UUID.randomUUID(), List.of(role), null);
    userEndpoint().createUser("alison.pritchard@ons.gov.uk", PHIL);
    assertTrue(userRepo.findByIdentity(PHIL).isPresent());
  }

  @Test
  public void shouldUndeleteUser() throws Exception {
    Role role =
        txOps.createRole("user-creator", UUID.randomUUID(), List.of(PermissionType.CREATE_USER));
    txOps.createUser("alison.pritchard@ons.gov.uk", UUID.randomUUID(), List.of(role), null);
    userEndpoint().createUser("alison.pritchard@ons.gov.uk", PHIL);
    assertTrue(userRepo.findByIdentity(PHIL).isPresent());

    userEndpoint().deleteUser("user.manager@ext.ons.gov.uk", PHIL);
    User user = txOps.findUser(PHIL);
    assertTrue(user.isDeleted());

    userEndpoint().createUser("alison.pritchard@ons.gov.uk", PHIL);
    user = txOps.findUser(PHIL);
    assertFalse(user.isDeleted());
  }

  @Test
  public void shouldUndeleteUserWithResetAtttributes() throws Exception {
    Role role1 =
        txOps.createRole("user-creator", UUID.randomUUID(), List.of(PermissionType.CREATE_USER));
    Role role2 =
        txOps.createRole("case-searcher", UUID.randomUUID(), List.of(PermissionType.SEARCH_CASES));
    Role role3 =
        txOps.createRole("case-viewer", UUID.randomUUID(), List.of(PermissionType.VIEW_CASE));

    txOps.createUser(PHIL, UUID.randomUUID(), List.of(role1, role2), List.of(role3));
    User user = txOps.findUser(PHIL);
    txOps.verifyNormalUserAndRole(PHIL, "user-creator");
    txOps.verifyNormalUserAndRole(PHIL, "case-searcher");
    txOps.verifyAdminUserAndRole(PHIL, "case-viewer");

    userEndpoint().deleteUser("user.manager@ext.ons.gov.uk", PHIL);
    user = txOps.findUser(PHIL);
    assertTrue(user.isDeleted());

    txOps.createUser("alison.pritchard@ons.gov.uk", UUID.randomUUID(), List.of(role1), null);
    userEndpoint().createUser("alison.pritchard@ons.gov.uk", PHIL);
    user = txOps.findUser(PHIL);
    assertFalse(user.isDeleted());
    assertTrue(user.isActive());
    txOps.verifyNoUserRoles(PHIL);
    txOps.verifyNoAdminRoles(PHIL);
  }

  @Test
  public void deleteUser() throws CTPException {

    userEndpoint().deleteUser("user.manager@ext.ons.gov.uk", "delete.target@ext.ons.gov.uk");

    // Confirm user no longer exists
    User user = userRepo.findByIdentity("delete.target@ext.ons.gov.uk").get();
    assertTrue(user.isDeleted());
  }

  @Test
  public void deleteUserWhoIsUnDeletable() throws CTPException {

    // Make sure user under test can login
    ResponseEntity<UserDTO> loginResponse =
        userEndpoint().login("delete.target@ext.ons.gov.uk", "delete", "target");
    assertFalse(loginResponse.getBody().isDeletable());

    // Attempt to delete the user
    ResponseEntity<String> response =
        userEndpoint()
            .deleteUser(
                HttpStatus.BAD_REQUEST,
                "user.manager@ext.ons.gov.uk",
                "delete.target@ext.ons.gov.uk");
    assertTrue(response.getBody().contains("User not deletable"), response.getBody());

    // Confirm user still exists
    User user = userRepo.findByIdentity("delete.target@ext.ons.gov.uk").get();
    assertFalse(user.isDeleted());
  }

  @Test
  public void deleteUserSelfDeleteBlocked() throws CTPException {
    // Attempt to delete the user
    ResponseEntity<String> response =
        userEndpoint()
            .deleteUser(
                HttpStatus.UNAUTHORIZED,
                "user.manager@ext.ons.gov.uk",
                "user.manager@ext.ons.gov.uk");
    assertTrue(
        response.getBody().contains("Self modification of user account not allowed"),
        response.getBody());

    // Confirm user still exists
    User user = userRepo.findByIdentity("user.manager@ext.ons.gov.uk").get();
    assertFalse(user.isDeleted());
  }

  @Test
  public void getDeletedUserReturnsNotFound() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk",
            HttpMethod.GET,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void modifyDeletedUserReturnsNotFound() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk",
            HttpMethod.PUT,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void addingRoleToDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk/addUserRole/role",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void removingRoleFromDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk/removeUserRole/role",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void addingAdminRoleToDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk/addAdminRole/role",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void removingAdminRoleFromDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users/deleted.user@ext.ons.gov.uk/removeAdminRole/role",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void addingSurveyToDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString()
                + "/ccsvc/users/deleted.user@ext.ons.gov.uk/addSurvey/"
                + SurveyType.SOCIAL,
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void removingSurveyFromDeletedUserBadRequest() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            base.toString()
                + "/ccsvc/users/deleted.user@ext.ons.gov.uk/removeSurvey/"
                + SurveyType.SOCIAL,
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  public void deletedUserNotReturnedByGetUsers() throws CTPException {
    txOps.createDeletedUser("deleted.user@ext.ons.gov.uk", UUID.randomUUID());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "user.manager@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<List<UserDTO>> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/users",
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<UserDTO>>() {},
            params);
    assertEquals(2, response.getBody().size());
    assertNotEquals("deleted.user@ext.ons.gov.uk", response.getBody().get(0).getIdentity());
    assertNotEquals("deleted.user@ext.ons.gov.uk", response.getBody().get(1).getIdentity());

    List<User> users = userRepo.findAll();
    assertEquals(3, users.size());
  }
}

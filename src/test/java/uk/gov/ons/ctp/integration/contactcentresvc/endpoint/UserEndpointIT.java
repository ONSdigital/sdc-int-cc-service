package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

public class UserEndpointIT extends FullStackIntegrationTestBase {

  private static final UUID DELETER_ROLE_ID =
      UUID.fromString("7121e404-9ec0-11ec-a16c-4c3275913db5");

  @Autowired private TransactionalOps txOps;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  public void setup() throws MalformedURLException {
    super.init();

    txOps.deleteAll();

    var perms = List.of(PermissionType.DELETE_USER);
    Role role = txOps.createRole("deleter", DELETER_ROLE_ID, perms);
    txOps.createUser("user.manager@ext.ons.gov.uk", UUID.randomUUID(), List.of(role), null);
    txOps.createUser("delete.target@ext.ons.gov.uk", UUID.randomUUID(), List.of(role), null);
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
}

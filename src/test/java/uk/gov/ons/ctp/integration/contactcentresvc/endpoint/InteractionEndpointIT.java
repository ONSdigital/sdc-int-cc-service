package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
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
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;

public class InteractionEndpointIT extends FullStackIntegrationTestBase {

  private static final UUID TARGET_USER = UUID.fromString("4f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID PRINCIPAL_USER =
      UUID.fromString("3f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID = UUID.fromString("5f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID_2 = UUID.fromString("6f27ee97-7ba7-4979-b9e8-bfe3063b41e8");

  @Autowired private TransactionalOps txOps;

  TestRestTemplate restTemplate;

  URL base;
  @LocalServerPort int port;

  @BeforeEach
  public void setup() throws MalformedURLException {
    base = new URL("http://localhost:" + port);

    restTemplate = new TestRestTemplate(new RestTemplateBuilder());

    txOps.deleteAll();
    txOps.setupSurveyAndCollex();
  }

  @Test
  public void getCorrectCaseInteractionsByUser() {
    var perms = List.of(PermissionType.READ_USER_INTERACTIONS);
    Role role = txOps.createRole("interaction_reader", TARGET_USER, perms);
    User otherUser =
        txOps.createUser("principal.user@ext.ons.gov.uk", TARGET_USER, List.of(role), null);
    User targetUser = txOps.createUser("target.user@ext.ons.gov.uk", PRINCIPAL_USER);
    Case case1 = txOps.createCase(CASE_ID);
    Case case2 = txOps.createCase(CASE_ID_2);

    txOps.createInteraction(case1, otherUser, LocalDateTime.now());
    txOps.createInteraction(case2, otherUser, LocalDateTime.now());

    txOps.createInteraction(case1, targetUser, LocalDateTime.of(2022, 1, 1, 1, 1));
    txOps.createInteraction(case2, targetUser, LocalDateTime.of(2021, 1, 1, 1, 1));
    txOps.createInteraction(case1, targetUser, LocalDateTime.of(2022, 3, 3, 3, 3));

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "principal.user@ext.ons.gov.uk");

    HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<List<UsersCaseInteractionDTO>> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/interactions/user/target.user@ext.ons.gov.uk",
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<UsersCaseInteractionDTO>>() {},
            params);

    assertEquals(3, response.getBody().size());
    assertEquals(
        LocalDateTime.of(2022, 3, 3, 3, 3), response.getBody().get(0).getCreatedDateTime());
    assertEquals(
        LocalDateTime.of(2022, 1, 1, 1, 1), response.getBody().get(1).getCreatedDateTime());
    assertEquals(
        LocalDateTime.of(2021, 1, 1, 1, 1), response.getBody().get(2).getCreatedDateTime());
  }
}

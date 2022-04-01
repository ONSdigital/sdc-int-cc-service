package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseInteractionRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

public class CaseEndpointInvalidateIT extends FullStackIntegrationTestBase {

  private static final UUID PRINCIPAL_USER =
      UUID.fromString("3f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final UUID CASE_ID = UUID.fromString("5f27ee97-7ba7-4979-b9e8-bfe3063b41e8");
  private static final String NOTE = "A note";

  @Autowired private TransactionalOps txOps;
  @Autowired private CaseInteractionRepository interactionRepository;

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
  public void invalidateExistingCase() {
    var perms = List.of(PermissionType.INVALIDATE_CASE);
    Role role = txOps.createRole("invalidate_case", PRINCIPAL_USER, perms);
    txOps.createSurveyUser("principal.user@ext.ons.gov.uk", PRINCIPAL_USER, List.of(role), null);
    txOps.createCase(CASE_ID);

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", "principal.user@ext.ons.gov.uk");

    InvalidateCaseRequestDTO invalidateCaseRequestDTO = new InvalidateCaseRequestDTO();
    invalidateCaseRequestDTO.setNote(NOTE);

    HttpEntity<?> requestEntity = new HttpEntity<>(invalidateCaseRequestDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<ResponseDTO> response =
        restTemplate.exchange(
            base.toString() + "/ccsvc/cases/" + CASE_ID + "/invalidate",
            HttpMethod.POST,
            requestEntity,
            ResponseDTO.class,
            params);

    List<CaseInteraction> interactions = interactionRepository.findAllByCazeId(CASE_ID);

    assertEquals(1, interactions.size());
    assertEquals(CASE_ID, interactions.get(0).getCaze().getId());
    assertEquals(NOTE, interactions.get(0).getNote());

    assertEquals(CASE_ID.toString(), response.getBody().getId());
  }
}

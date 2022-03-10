package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CreateRoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;

public class RoleEndpointCaller {

  URL baseURL;
  TestRestTemplate restTemplate; 
  
  public RoleEndpointCaller(URL baseURL) {
    this.baseURL = baseURL;
    this.restTemplate = new TestRestTemplate(new RestTemplateBuilder());
  }

  /*
   * POST to /ccsvc/roles, to create a new role.
   */
  public ResponseEntity<RoleDTO> invokeCreateRole(String principle, String name, String description) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    CreateRoleDTO newRoleDTO = new CreateRoleDTO();
    newRoleDTO.setName(name);
    newRoleDTO.setDescription(description);
    
    HttpEntity<CreateRoleDTO> requestEntity =
        new HttpEntity<CreateRoleDTO>(newRoleDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    ResponseEntity<RoleDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/roles",
            HttpMethod.POST,
            requestEntity,
            RoleDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
  
  /*
   * PATCH to /ccsvc/{roleName}/addPermission/{permissionType}, to add a permission to a role. 
   */
  public ResponseEntity<RoleDTO> invokeAddPermission(String principle, String roleName, PermissionType permission) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    HttpEntity<RoleDTO> requestEntity = new HttpEntity<RoleDTO>(headers);

    Map<String, String> params = new HashMap<String, String>();
    params.put("roleName", roleName);
    params.put("permissionType", permission.name());
    
    ResponseEntity<RoleDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/roles/{roleName}/addPermission/{permissionType}",
            HttpMethod.PATCH,
            requestEntity,
            RoleDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
}

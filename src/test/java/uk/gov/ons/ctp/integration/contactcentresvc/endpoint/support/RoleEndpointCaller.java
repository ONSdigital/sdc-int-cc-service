package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.resource.beans.container.internal.CdiBeanContainerDelayedAccessImpl;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;

import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

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
  public ResponseEntity<RoleDTO> invokeCreateRole(String principle, String name, String description, PermissionType... permissions ) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    RoleDTO newRoleDTO = new RoleDTO();
    newRoleDTO.setName(name);
    newRoleDTO.setDescription(description);
    newRoleDTO.setPermissions(List.of(permissions));
    
    HttpEntity<RoleDTO> requestEntity =
        new HttpEntity<RoleDTO>(newRoleDTO, headers);

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

package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CreateRoleDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RoleDTO;

public class RoleEndpointCaller {

  private EndpointCaller endpointCaller;

  public RoleEndpointCaller(URL baseURL) {
    this.endpointCaller = new EndpointCaller(baseURL);
  }

  /*
   * POST to /ccsvc/roles
   */
  public ResponseEntity<RoleDTO> createRole(String principle, String name, String description) {

    CreateRoleDTO newRoleDTO = new CreateRoleDTO();
    newRoleDTO.setName(name);
    newRoleDTO.setDescription(description);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.POST,
        "/ccsvc/roles",
        new ParameterizedTypeReference<RoleDTO>() {},
        newRoleDTO,
        principle);
  }

  /*
   * PATCH to /ccsvc/{roleName}/addPermission/{permissionType}
   */
  public ResponseEntity<RoleDTO> addPermission(
      String principle, String roleName, PermissionType permission) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("roleName", roleName);
    params.put("permissionType", permission.name());

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.PATCH,
        "/ccsvc/roles/{roleName}/addPermission/{permissionType}",
        new ParameterizedTypeReference<RoleDTO>() {},
        null,
        principle,
        params);
  }
}

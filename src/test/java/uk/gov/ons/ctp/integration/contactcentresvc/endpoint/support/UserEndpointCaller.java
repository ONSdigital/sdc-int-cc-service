package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

public class UserEndpointCaller {

  private EndpointCaller endpointCaller;

  public UserEndpointCaller(URL baseURL) {
    this.endpointCaller = new EndpointCaller(baseURL);
  }

  /*
   * 200 status PUT for /ccsvc/users/login
   */
  public ResponseEntity<UserDTO> login(String userIdentity, String forename, String surname) {

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename(forename);
    loginRequestDTO.setSurname(surname);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.PUT,
        "/ccsvc/users/login",
        new ParameterizedTypeReference<UserDTO>() {},
        loginRequestDTO,
        userIdentity);
  }

  /*
   * 200 status PUT for /ccsvc/users/logout
   */
  public ResponseEntity<UserDTO> logout(String userIdentity) {

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.PUT,
        "/ccsvc/users/logout",
        new ParameterizedTypeReference<UserDTO>() {},
        null,
        userIdentity);
  }

  /*
   * 200 status POST to /ccsvc/users
   */
  public ResponseEntity<UserDTO> createUser(String principle, String userIdentity) {

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.POST,
        "/ccsvc/users",
        new ParameterizedTypeReference<UserDTO>() {},
        userDTO,
        principle);
  }

  /*
   * 200 status PATCH for /ccsvc/users/{userIdentity}/addUserRole/{roleName}
   */
  public ResponseEntity<UserDTO> addUserRole(
      String principle, String userIdentity, String roleName) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.PATCH,
        "/ccsvc/users/{userIdentity}/addUserRole/{roleName}",
        new ParameterizedTypeReference<UserDTO>() {},
        null,
        principle,
        params);
  }

  /*
   * 200 status PATCH for /ccsvc/users/{userIdentity}/addAdminRole/{roleName}
   */
  public ResponseEntity<UserDTO> addAdminRole(
      String principle, String userIdentity, String roleName) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.PATCH,
        "/ccsvc/users/{userIdentity}/addAdminRole/{roleName}",
        new ParameterizedTypeReference<UserDTO>() {},
        null,
        principle,
        params);
  }

  /*
   * 200 status GET for /ccsvc/users/audit
   */
  public ResponseEntity<List<UserAuditDTO>> searchAudit(
      String userIdentity, String principle, String targetUser) {

    return doInvokeAudit(
        HttpStatus.OK,
        new ParameterizedTypeReference<List<UserAuditDTO>>() {},
        userIdentity,
        principle,
        targetUser);
  }

  /*
   * Failure path GET for /ccsvc/users/audit.
   */
  public ResponseEntity<String> searchAudit(
      HttpStatus expectedHttpStatus, String userIdentity, String principle, String targetUser) {

    return doInvokeAudit(
        expectedHttpStatus,
        new ParameterizedTypeReference<String>() {},
        userIdentity,
        principle,
        targetUser);
  }

  private <T> ResponseEntity<T> doInvokeAudit(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String userIdentity,
      String principle,
      String targetUser) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("principle", principle);
    params.put("targetUser", targetUser);

    return endpointCaller.invokeEndpoint(
        expectedHttpStatus,
        HttpMethod.GET,
        "/ccsvc/users/audit?principle={principle}&targetUser={targetUser}",
        responseType,
        null,
        userIdentity,
        params);
  }
}

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

/** Utility class which allows integration tests to easily call the User based endpoints. */
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
   * 200 status DELETE to /ccsvc/users/
   */
  public ResponseEntity<UserDTO> deleteUser(String principle, String userIdentity) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);

    return endpointCaller.invokeEndpoint(
        HttpStatus.OK,
        HttpMethod.DELETE,
        "/ccsvc/users/{userIdentity}",
        new ParameterizedTypeReference<UserDTO>() {},
        null,
        principle,
        params);
  }

  /*
   * Error response for DELETE to /ccsvc/users/
   */
  public ResponseEntity<String> deleteUser(
      HttpStatus expectedHttpStatus, String principle, String userIdentity) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);

    return endpointCaller.invokeEndpoint(
        expectedHttpStatus,
        HttpMethod.DELETE,
        "/ccsvc/users/{userIdentity}",
        new ParameterizedTypeReference<String>() {},
        null,
        principle,
        params);
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
      String principle, String performedBy, String performedOn) {

    return doInvokeAudit(
        HttpStatus.OK,
        new ParameterizedTypeReference<List<UserAuditDTO>>() {},
        principle,
        performedBy,
        performedOn);
  }

  /*
   * Failure path GET for /ccsvc/users/audit.
   */
  public ResponseEntity<String> searchAudit(
      HttpStatus expectedHttpStatus, String principle, String performedBy, String performedOn) {

    return doInvokeAudit(
        expectedHttpStatus,
        new ParameterizedTypeReference<String>() {},
        principle,
        performedBy,
        performedOn);
  }

  private <T> ResponseEntity<T> doInvokeAudit(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String principle,
      String performedBy,
      String performedOn) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("performedBy", performedBy);
    params.put("performedOn", performedOn);

    return endpointCaller.invokeEndpoint(
        expectedHttpStatus,
        HttpMethod.GET,
        "/ccsvc/users/audit?performedBy={performedBy}&performedOn={performedOn}",
        responseType,
        null,
        principle,
        params);
  }
}

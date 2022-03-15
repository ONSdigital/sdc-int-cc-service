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
  public ResponseEntity<UserDTO> login(String userIdentity) {

    return doLogin(HttpStatus.OK,
        new ParameterizedTypeReference<UserDTO>() {}, userIdentity, "John", "Smith");
  }

  private <T> ResponseEntity<T> doLogin(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String userIdentity, String forename, String surname) {

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename(forename);
    loginRequestDTO.setSurname(surname);

    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.PUT, "/ccsvc/users/login", responseType, loginRequestDTO, userIdentity);
  }

  /*
   * 200 status PUT for /ccsvc/users/logout
   */
  public ResponseEntity<UserDTO> logout(String userIdentity) {

    return doLogout(HttpStatus.OK,
        new ParameterizedTypeReference<UserDTO>() {}, userIdentity);
  }

  private <T> ResponseEntity<T> doLogout(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String userIdentity) {

    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.PUT, "/ccsvc/users/logout", responseType, null, userIdentity);
  }

  /*
   * 200 status POST to /ccsvc/users
   */
  public ResponseEntity<UserDTO> createUser(
      String principle, String userIdentity) {

    return doCreateUser(HttpStatus.OK,
        new ParameterizedTypeReference<UserDTO>() {}, principle, userIdentity);
  }

  private <T> ResponseEntity<T> doCreateUser(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String principle, String userIdentity) {

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);

    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.POST, "/ccsvc/users", responseType, userDTO, principle);
  }

  /*
   * 200 status PATCH for /ccsvc/users/{userIdentity}/addUserRole/{roleName}
   */
  public ResponseEntity<UserDTO> addUserRole(
      String principle, String userIdentity, String roleName) {

    return doAddUserRole(HttpStatus.OK,
        new ParameterizedTypeReference<UserDTO>() {}, principle, userIdentity, roleName);
  }

  private <T> ResponseEntity<T> doAddUserRole(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String principle, String userIdentity, String roleName) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);
    
    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.PATCH, "/ccsvc/users/{userIdentity}/addUserRole/{roleName}", responseType, null, principle, params);
  }

  /*
   * 200 status PATCH for /ccsvc/users/{userIdentity}/addAdminRole/{roleName}
   */
  public ResponseEntity<UserDTO> addAdminRole(
      String principle, String userIdentity, String roleName) {

    return doAddAdminRole(HttpStatus.OK,
        new ParameterizedTypeReference<UserDTO>() {}, principle, userIdentity, roleName);
  }

  private <T> ResponseEntity<T> doAddAdminRole(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String principle, String userIdentity, String roleName) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);
    
    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.PATCH, "/ccsvc/users/{userIdentity}/addAdminRole/{roleName}", responseType, null, principle, params);
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

    return endpointCaller.invokeEndpoint(expectedHttpStatus, HttpMethod.GET, "/ccsvc/users/audit?principle={principle}&targetUser={targetUser}", responseType, null, userIdentity, params);
  }
}

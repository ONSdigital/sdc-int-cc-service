package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.ons.ctp.integration.contactcentresvc.representation.LoginRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;

public class UserEndpointCaller {

  URL baseURL;
  TestRestTemplate restTemplate; 
  
  public UserEndpointCaller(URL baseURL) {
    this.baseURL = baseURL;
    this.restTemplate = new TestRestTemplate(new RestTemplateBuilder());
  }

  /**
   * PUT to /ccsvc/users/login.
   *  
   * @param userIdentity is the user logging in.
   * @return UserDTO for the user.
   */
  public ResponseEntity<UserDTO> invokeLogin(String userIdentity) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
    loginRequestDTO.setForename("John");
    loginRequestDTO.setSurname("Smith");

    HttpEntity<LoginRequestDTO> requestEntity =
        new HttpEntity<LoginRequestDTO>(loginRequestDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/login",
            HttpMethod.PUT,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
  
  /*
   * PUT to /ccsvc/users/login.
   */
  public ResponseEntity<UserDTO> invokeLogout(String userIdentity) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    HttpEntity<LoginRequestDTO> requestEntity =
        new HttpEntity<LoginRequestDTO>(headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/logout",
            HttpMethod.PUT,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
  
  /**
   * POST to /ccsvc/users.
   * 
   * This creates a new user.
   * 
   * @param principle is the user doing the creation.
   * @param userIdentity is the name of the user to create.
   * @return UserDTO for the new user.
   */
  public ResponseEntity<UserDTO> invokeCreateUser(String principle, String userIdentity) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);

    HttpEntity<UserDTO> requestEntity = new HttpEntity<UserDTO>(userDTO, headers);

    Map<String, String> params = new HashMap<String, String>();

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users",
            HttpMethod.POST,
            requestEntity,
            UserDTO.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
  
  /**
   * PATCH for /ccsvc/users/{userIdentity}/addUserRole/{roleName}
   */
  public ResponseEntity<UserDTO> addUserRole(String principle, String userIdentity, String roleName) {
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);

    HttpEntity<UserDTO> requestEntity = new HttpEntity<UserDTO>(userDTO, headers);

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/{userIdentity}/addUserRole/{roleName}",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }

  /**
   * PATCH for /ccsvc/users/{userIdentity}/addAdminRole/{roleName}
   */
  public ResponseEntity<UserDTO> addAdminRole(String principle, String userIdentity, String roleName) {
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", principle);

    UserDTO userDTO = new UserDTO();
    userDTO.setIdentity(userIdentity);

    HttpEntity<UserDTO> requestEntity = new HttpEntity<UserDTO>(userDTO, headers);

    Map<String, String> params = new HashMap<String, String>();
    params.put("userIdentity", userIdentity);
    params.put("roleName", roleName);

    // Submit login request
    ResponseEntity<UserDTO> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/{userIdentity}/addAdminRole/{roleName}",
            HttpMethod.PATCH,
            requestEntity,
            UserDTO.class,
            params);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    return response;
  }
  
  /**
   * GET for /ccsvc/users/audit.
   */
  public ResponseEntity<String> invokeAudit(HttpStatus expectedHttpStatus, String userIdentity, String principle, String targetUser) {

    return doInvokeAudit(expectedHttpStatus, new ParameterizedTypeReference<String>() {}, userIdentity, principle, targetUser);
  }

  /**
   * 200 status GET for /ccsvc/users/audit.
   */
  public ResponseEntity<List<UserAuditDTO>> invokeAudit(String userIdentity, String principle, String targetUser) {

    return doInvokeAudit(HttpStatus.OK, new ParameterizedTypeReference<List<UserAuditDTO>>() {}, userIdentity, principle, targetUser);
  }
    
  private <T> ResponseEntity<T> doInvokeAudit(HttpStatus expectedHttpStatus, ParameterizedTypeReference<T> responseType, String userIdentity, String principle, String targetUser) {

    Map<String, String> params = new HashMap<String, String>();
    params.put("principle", principle);
    params.put("targetUser", targetUser);
    
    return invokeEndpoint(expectedHttpStatus, responseType, userIdentity, params);
  }


  private <T> ResponseEntity<T> invokeEndpoint(HttpStatus expectedHttpStatus, ParameterizedTypeReference<T> responseType, String userIdentity, Map<String, String> params) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);

    ResponseEntity<T> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/audit?principle={principle}&targetUser={targetUser}",
            HttpMethod.GET,
            requestEntity,
            responseType, //new ParameterizedTypeReference<List<UserAuditDTO>>() {},
            params);

    assertEquals(expectedHttpStatus, response.getStatusCode());
    
    return response;
  }
}

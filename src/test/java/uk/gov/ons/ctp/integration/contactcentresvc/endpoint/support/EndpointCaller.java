package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EndpointCaller {

  private URL baseURL;
  private TestRestTemplate restTemplate;

  public EndpointCaller(URL baseURL, TestRestTemplate restTemplate) {
    this.baseURL = baseURL;
    this.restTemplate = restTemplate;
  }
  
  public <T> ResponseEntity<T> invokeEndpoint(
      HttpStatus expectedHttpStatus,
      ParameterizedTypeReference<T> responseType,
      String userIdentity,
      Map<String, String> params) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);

    ResponseEntity<T> response =
        restTemplate.exchange(
            baseURL.toString() + "/ccsvc/users/audit?principle={principle}&targetUser={targetUser}",
            HttpMethod.GET,
            requestEntity,
            responseType,
            params);

    assertEquals(expectedHttpStatus, response.getStatusCode());

    return response;
  }
}

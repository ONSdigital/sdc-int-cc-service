package uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EndpointCaller {

  private URL baseURL;
  private TestRestTemplate restTemplate;
  
  public EndpointCaller(URL baseURL) {
    this.baseURL = baseURL;
    this.restTemplate = new TestRestTemplate(new RestTemplateBuilder());
  }
  
  public <T> ResponseEntity<T> invokeEndpoint(
      HttpStatus expectedHttpStatus,
      HttpMethod httpMethod,
      String url,
      ParameterizedTypeReference<T> responseType,
      Object requestData,
      String userIdentity,
      Map<String, String> params) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-user-id", userIdentity);

    HttpEntity<?> requestEntity = new HttpEntity<Object>(requestData, headers);

    ResponseEntity<T> response =
        restTemplate.exchange(
            baseURL.toString() + url,
            httpMethod,
            requestEntity,
            responseType,
            params);

    assertEquals(expectedHttpStatus, response.getStatusCode());

    return response;
  }
  
  // Variant with no params
  public <T> ResponseEntity<T> invokeEndpoint(
      HttpStatus expectedHttpStatus,
      HttpMethod httpMethod,
      String url,
      ParameterizedTypeReference<T> responseType,
      Object requestData,
      String userIdentity) {

   Map<String, String> params = new HashMap<String, String>();
    
   return invokeEndpoint(expectedHttpStatus, httpMethod, url, responseType,  requestData, userIdentity, params);
  }
}

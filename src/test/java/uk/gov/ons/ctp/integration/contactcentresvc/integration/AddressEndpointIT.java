package uk.gov.ons.ctp.integration.contactcentresvc.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.AddressEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;

/**
 * This class holds integration tests that submit requests to the Contact Centre service, which in
 * turn will delegating the query to the Address Index service.
 */
@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public final class AddressEndpointIT {
  @Autowired private AddressEndpoint addressEndpoint;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  public void setUp() throws Exception {
    this.mockMvc = MockMvcBuilders.standaloneSetup(addressEndpoint).build();
  }

  /**
   * This test submits a generic address query and validates that some data is returned in the
   * expected format. Without a fixed test data set this is really as much validation as it can do.
   */
  @Test
  public void validateAddressQueryResponse() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/addresses?input=Street")).andExpect(status().isOk()).andReturn();
    String jsonResponse = result.getResponse().getContentAsString();

    verifyJsonInAddressQueryResponseFormat(jsonResponse, 1, 100);
  }

  /**
   * This test validates that result pagination (controlled by the 'offset' and 'limit' parameters)
   * is working. It firstly captures the result of a query and then confirms that subsets of these
   * results can be fetched.
   */
  @Test
  public void validateAddressQueryPagination() throws Exception {
    String baseUrl = "/addresses?input=Street";
    doPaginationTest(baseUrl);
  }

  /** This test runs an address query which should get 0 results. */
  @Test
  public void validateAddressQueryEmptyResponse() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/addresses?input=euooeuuoeueoueou"))
            .andExpect(status().isOk())
            .andReturn();
    String jsonResponse = result.getResponse().getContentAsString();

    verifyJsonInAddressQueryResponseFormat(jsonResponse, 0, 0);
  }

  /**
   * This test submits a generic address query and validates that some data is returned in the
   * expected format. Without a fixed test data set this is really as much validation as it can do.
   */
  @Test
  public void validatePostcodeQueryResponse() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/addresses/postcode?postcode=EX2 4LU"))
            .andExpect(status().isOk())
            .andReturn();
    String jsonResponse = result.getResponse().getContentAsString();

    verifyJsonInAddressQueryResponseFormat(jsonResponse, 1, 100);
  }

  /**
   * This test validates that result pagination (controlled by the 'offset' and 'limit' parameters)
   * is working. It firstly captures the result of a query and then confirms that subsets of these
   * results can be fetched.
   */
  @Test
  public void validatePostcodeQueryPagination() throws Exception {
    String baseUrl = "/addresses/postcode?postcode=EX2 4LU";
    doPaginationTest(baseUrl);
  }

  /** This test submits a postcode query which should get 0 results */
  @Test
  public void validatePostcodeQueryEmptyResponse() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/addresses/postcode?postcode=ZE1 9XX"))
            .andExpect(status().isOk())
            .andReturn();
    String jsonResponse = result.getResponse().getContentAsString();

    verifyJsonInAddressQueryResponseFormat(jsonResponse, 0, 0);
  }

  private void doPaginationTest(String baseUrl) throws Exception {
    // Firstly get a full set of results
    AddressQueryResponseDTO fullResults = runAddressBasedQuery(baseUrl);

    // Now step through subsets of the same query. The full results are assumed to be correct
    int offset = 0;
    int limit = 26;
    int resultsTotal = fullResults.getAddresses().size();
    while (offset < resultsTotal) {
      limit = offset + limit >= resultsTotal ? resultsTotal - offset : 26;

      String pageUrl = baseUrl + "&offset=" + offset + "&limit=" + limit;
      AddressQueryResponseDTO pageResults = runAddressBasedQuery(pageUrl);

      // Verify that this page of results matches the subset of the full results
      for (int i = 0; i < limit; i++) {
        AddressDTO expected = fullResults.getAddresses().get(i + offset);
        AddressDTO actual = pageResults.getAddresses().get(i);

        assertEquals(expected.getUprn(), actual.getUprn());
        assertEquals(expected.getFormattedAddress(), actual.getFormattedAddress());
        assertEquals(expected.getWelshFormattedAddress(), actual.getWelshFormattedAddress());
      }

      offset += limit;
    }
  }

  // This method validates that the supplied json string is in the address query response (as
  // shown in the swagger)
  private void verifyJsonInAddressQueryResponseFormat(String json, int minExpected, int maxExpected)
      throws IOException, JsonParseException, JsonMappingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(json, JsonNode.class);

    int dataVersion = Integer.parseInt(jsonNode.get("dataVersion").asText());
    assertTrue(dataVersion >= 39, () -> "Wrong dataVersion: " + dataVersion);

    JsonNode addresses = jsonNode.get("addresses");
    Iterator<JsonNode> iter = addresses.iterator();
    while (iter.hasNext()) {
      JsonNode addressNode = iter.next();

      JsonNode uprn = addressNode.get("uprn");
      assertFalse(uprn.textValue().equals("0"), uprn.textValue());

      String region = addressNode.get("region").asText();
      assertFalse(region.isEmpty());

      String addressType = addressNode.get("addressType").asText();
      assertFalse(addressType.isEmpty());

      String estabType = addressNode.get("estabType").asText();
      assertFalse(estabType.isEmpty());

      String formattedAddress = addressNode.get("formattedAddress").asText();
      assertFalse(formattedAddress.isEmpty());

      String welshAddress = addressNode.get("welshFormattedAddress").asText();
      assertFalse(welshAddress.isEmpty());

      assertEquals(6, addressNode.size());
    }
    assertTrue(
        addresses.size() >= minExpected, "Not enough addresses found. Actual: " + addresses.size());
    assertTrue(
        addresses.size() <= maxExpected, "Too many addresses found. Actual: " + addresses.size());

    int totalNode = jsonNode.get("total").intValue();
    assertTrue(totalNode >= minExpected, "Query returned only a few addresses:" + totalNode);
    assertTrue(
        addresses.size() <= totalNode,
        "Total too small. Have " + addresses.size() + " addresess but total is " + totalNode);

    assertEquals(3, jsonNode.size());
  }

  // Run a request and return the results as an object
  private AddressQueryResponseDTO runAddressBasedQuery(String url) throws Exception {
    MvcResult paginationResult = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();

    String json = paginationResult.getResponse().getContentAsString();
    AddressQueryResponseDTO results = mapper.readValue(json, AddressQueryResponseDTO.class);
    return results;
  }
}

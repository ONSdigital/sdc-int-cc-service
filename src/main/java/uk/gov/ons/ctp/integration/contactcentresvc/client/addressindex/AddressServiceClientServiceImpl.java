package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;

/** This class is responsible for communications with the Address Index service. */
@Slf4j
@Service
@Validated
public class AddressServiceClientServiceImpl {
  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  public AddressIndexSearchResultsDTO searchByAddress(AddressQueryRequestDTO addressQueryRequest) {
    log.debug("Delegating address search to AddressIndex service");

    String input = addressQueryRequest.getInput().trim();
    int offset = addressQueryRequest.getOffset();
    int limit = addressQueryRequest.getLimit();

    // Address query is delegated to Address Index. Build the query params for the request
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("input", input);
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));
    queryParams.add("historical", "false");
    queryParams.add("includeauxiliarysearch", "true");
    queryParams.add("matchthreshold", "0");
    addEpoch(queryParams);

    // Ask Address Index to do an address search
    String path = appConfig.getAddressIndexSettings().getAddressQueryPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, new Object[] {});
    if (log.isDebugEnabled()) {
      log.debug(
          "Address query response received",
          kv("status", addressIndexResponse.getStatus().getCode()),
          kv("addresses", addressIndexResponse.getResponse().getAddresses().size()));
    }

    return addressIndexResponse;
  }

  public AddressIndexSearchResultsDTO searchByPostcode(
      PostcodeQueryRequestDTO postcodeQueryRequest) {
    if (log.isDebugEnabled()) {
      log.debug("Delegating postcode search to the AddressIndex service");
    }

    int offset = postcodeQueryRequest.getOffset();
    int limit = postcodeQueryRequest.getLimit();

    // Postcode query is delegated to Address Index. Build the query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));
    queryParams.add("includeauxiliarysearch", "true");
    addEpoch(queryParams);

    // Ask Address Index to do postcode search
    String postcode = postcodeQueryRequest.getPostcode();
    String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, postcode);
    if (log.isDebugEnabled()) {
      log.debug(
          "Postcode query response received",
          kv("postcode", postcode),
          kv("status", addressIndexResponse.getStatus().getCode()),
          kv("addresses", addressIndexResponse.getResponse().getAddresses().size()));
    }

    return addressIndexResponse;
  }

  public AddressIndexSearchResultsCompositeDTO searchByUPRN(Long uprn) {
    log.debug("Delegating UPRN search to AddressIndex service");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("addresstype", appConfig.getAddressIndexSettings().getAddressType());
    addEpoch(queryParams);

    // Ask Address Index to do uprn search
    String path = appConfig.getAddressIndexSettings().getUprnLookupPath();
    AddressIndexSearchResultsCompositeDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsCompositeDTO.class, null, queryParams, uprn.toString());

    if (log.isDebugEnabled()) {
      log.debug(
          "UPRN query response received",
          kv("uprn", uprn),
          kv("status", addressIndexResponse.getStatus().getCode()));
    }

    return addressIndexResponse;
  }

  private MultiValueMap<String, String> addEpoch(MultiValueMap<String, String> queryParams) {
    String epoch = appConfig.getAddressIndexSettings().getEpoch();
    if (!StringUtils.isBlank(epoch)) {
      queryParams.add("epoch", epoch);
    }
    return queryParams;
  }
}

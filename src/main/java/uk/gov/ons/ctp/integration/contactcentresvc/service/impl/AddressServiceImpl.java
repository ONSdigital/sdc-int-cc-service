package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.util.StringUtils;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

/**
 * A ContactCentreDataService implementation which encapsulates all business logic for getting
 * Addresses
 */
@Slf4j
@Service
@Validated
public class AddressServiceImpl implements AddressService {
  private static final String HISTORICAL_ADDRESS_STATUS = "8";

  @Autowired private AddressServiceClientServiceImpl addressServiceClient;

  @Override
  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest) {
    if (log.isDebugEnabled()) {
      log.debug("Running search by address", kv("addressQueryRequest", addressQueryRequest));
    }

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByAddress(addressQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    if (log.isDebugEnabled()) {
      log.debug(
          "Address search is returning addresses", kv("addresses", results.getAddresses().size()));
    }
    return results;
  }

  @Override
  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {
    if (log.isDebugEnabled()) {
      log.debug("Running search by postcode", kv("postcodeQueryRequest", postcodeQueryRequest));
    }

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByPostcode(postcodeQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    if (log.isDebugEnabled()) {
      log.debug(
          "Postcode search is returning addresses", kv("addresses", results.getAddresses().size()));
    }
    return results;
  }

  @Override
  public AddressIndexAddressCompositeDTO uprnQuery(long uprn) throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Running search by uprn", kv("uprnQueryRequest", uprn));
    }

    // Delegate the query to Address Index
    try {
      AddressIndexSearchResultsCompositeDTO addressResult = addressServiceClient.searchByUPRN(uprn);
      // No result for UPRN from Address Index search
      if (addressResult.getStatus().getCode() != 200) {
        log.warn(
            "UPRN not found calling Address Index",
            kv("uprn", uprn),
            kv("status", addressResult.getStatus().getCode()),
            kv("message", addressResult.getStatus().getMessage()));
        throw new CTPException(
            CTPException.Fault.RESOURCE_NOT_FOUND,
            "UPRN: %s, status: %s, message: %s",
            uprn,
            addressResult.getStatus().getCode(),
            addressResult.getStatus().getMessage());
      }

      AddressIndexAddressCompositeDTO address = addressResult.getResponse().getAddress();

      if (log.isDebugEnabled()) {
        log.debug("UPRN search is returning address", kv("uprn", uprn));
      }
      return address;
    } catch (ResponseStatusException ex) {
      log.warn(
          "UPRN not found calling Address Index",
          kv("uprn", uprn),
          kv("status", ex.getStatus()),
          kv("message", ex.getMessage()));
      throw ex;
    }
  }

  private AddressDTO convertToSummarised(AddressIndexAddressDTO fullAddress) {
    String formattedAddress = fullAddress.getFormattedAddress();
    String addressPaf = fullAddress.getFormattedAddressPaf();
    String addressNag = fullAddress.getFormattedAddressNag();
    String welshAddressPaf = fullAddress.getWelshFormattedAddressPaf();
    String welshAddressNag = fullAddress.getWelshFormattedAddressNag();
    String estabDescription = fullAddress.getCensus().getEstabType();

    AddressDTO addressSummary = new AddressDTO();
    addressSummary.setUprn(fullAddress.getUprn());
    addressSummary.setRegion(fullAddress.getCensus().getCountryCode());
    addressSummary.setAddressType(fullAddress.getCensus().getAddressType());
    addressSummary.setEstabType(EstabType.forCode(estabDescription).name());
    addressSummary.setEstabDescription(estabDescription);
    addressSummary.setFormattedAddress(
        StringUtils.selectFirstNonBlankString(addressPaf, addressNag, formattedAddress));
    addressSummary.setWelshFormattedAddress(
        StringUtils.selectFirstNonBlankString(welshAddressPaf, welshAddressNag, formattedAddress));
    return addressSummary;
  }

  /**
   * Determine whether an address returned from AIMS is historical.
   *
   * <p>In reality, we should never get historical addresses from AIMS. However since it is so
   * important not to return historical addresses, we accept the pagination breakage to filter out
   * any that we find. The theory is that logging errors will notify operations to fix AIMS if it is
   * not honouring the historical=false query parameter, and the service will be rectified as a
   * result.
   *
   * <p>See CR-976.
   *
   * @param dto the address from AIMS
   * @return true if historical; false otherwise.
   */
  private boolean isHistorical(AddressIndexAddressDTO dto) {
    boolean historical = HISTORICAL_ADDRESS_STATUS.equals(dto.getLpiLogicalStatus());
    if (historical) {
      log.error(
          "Unexpected historical address returned from AIMS",
          kv("uprn", dto.getUprn()),
          kv("formattedAddress", dto.getFormattedAddress()));
    }
    return historical;
  }

  private AddressQueryResponseDTO convertAddressIndexResultsToSummarisedAdresses(
      AddressIndexSearchResultsDTO addressIndexResponse) {
    List<AddressDTO> summarisedAddresses =
        addressIndexResponse.getResponse().getAddresses().stream()
            .filter(a -> !isHistorical(a))
            .map(this::convertToSummarised)
            .collect(toList());

    // Allow Serco to handle NA addresses by reclassifying as HH
    for (AddressDTO address : summarisedAddresses) {
      String addressType = address.getAddressType();
      if (addressType != null && addressType.equals("NA")) {
        log.debug("Reclassifying NA address as HH", kv("uprn", address.getUprn()));
        address.setAddressType(AddressType.HH.name());
        address.setEstabType(EstabType.HOUSEHOLD.name());
        address.setEstabDescription("Household");
      }
    }

    // Complete construction of response objects
    AddressQueryResponseDTO queryResponse = new AddressQueryResponseDTO();
    queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
    queryResponse.setAddresses(new ArrayList<>(summarisedAddresses));

    int total = addressIndexResponse.getResponse().getTotal();
    int arraySize = summarisedAddresses.size();

    // UPRN search has no JSON total attribute as only one or zero
    if (total > 0) {
      queryResponse.setTotal(total);
    } else {
      queryResponse.setTotal(arraySize);
    }

    return queryResponse;
  }
}

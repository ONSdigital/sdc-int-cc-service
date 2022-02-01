package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressService;

/** The REST endpoint controller for ContactCentreSvc Details */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/addresses", produces = "application/json")
public final class AddressEndpoint implements CTPEndpoint {
  private AddressService addressService;

  /**
   * Constructor for ContactCentreDataEndpoint
   *
   * @param addressService is the object that this endpoint can call for address and postcode
   *     searches.
   */
  @Autowired
  public AddressEndpoint(final AddressService addressService) {
    this.addressService = addressService;
  }

  /**
   * This GET endpoint returns the addresses for an address search. If no matches are found then it
   * returns with 0 addresses, otherwise it returns with 1 or more addresses.
   *
   * @param addressQueryRequest is a DTO specify details on the address to search for.
   * @return an object listing the addresses matching the address search string.
   * @throws CTPException on error
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesBySearchQuery(
      @Valid AddressQueryRequestDTO addressQueryRequest) throws CTPException {
    log.info("Entering GET getAddressesBySearchQuery", kv("requestParams", addressQueryRequest));

    String addressQueryInput =
        addressQueryRequest.getInput().trim().replaceAll("'", "").replaceAll(",", "").trim();

    if (addressQueryInput.length() < 5) {
      throw new CTPException(
          Fault.BAD_REQUEST,
          "Address query requires 5 or more characters, "
              + "not including single quotes, commas or leading/trailing whitespace");
    }

    return addressService.addressQuery(addressQueryRequest);
  }

  /**
   * This GET endpoint returns the addresses for the specified postcode.
   *
   * @param postcodeQueryRequest is a DTO specify details on the postcode to search for.
   * @return an object listing the addresses for the postcode.
   * @throws CTPException if something goes wrong.
   */
  @RequestMapping(value = "/postcode", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesByPostcode(
      @Valid PostcodeQueryRequestDTO postcodeQueryRequest) throws CTPException {
    log.info("Entering GET getAddressesByPostcode", kv("requestParams", postcodeQueryRequest));

    return addressService.postcodeQuery(postcodeQueryRequest);
  }
}

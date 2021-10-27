package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.VersionResponseDTO;

/** The REST endpoint controller for ContactCentreSvc Version Details */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class VersionEndpoint implements CTPEndpoint {
  // The /version endpoint has been hardcoded as part of SOCINT-141 so that a value is returned. I'd
  // imagine that additional functionality will be added later.
  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   */
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public VersionResponseDTO getVersion() {
    log.info("Entering GET getVersion");
    VersionResponseDTO fakeVersion = VersionResponseDTO.builder().apiVersion("1.0.0").build();
    return fakeVersion;
  }
}

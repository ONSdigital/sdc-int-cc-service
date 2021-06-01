package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.Builder;
import lombok.Data;

/** This class holds data for the Contact Centre version endpoint. */
@Data
@Builder
public class VersionResponseDTO {
  String apiVersion;
}

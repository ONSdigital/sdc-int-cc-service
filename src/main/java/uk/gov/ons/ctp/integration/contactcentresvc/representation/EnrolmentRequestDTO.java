package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Holds details needed to enrol an individual against a parent household caes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolmentRequestDTO {

  private Map<String, Object> sampleSensitive;

  private Map<String, Object> consent;

  private Map<String, String> preferences;
}

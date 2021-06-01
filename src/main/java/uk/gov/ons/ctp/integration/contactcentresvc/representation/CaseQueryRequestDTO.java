package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object for a CaseDTOs case event data
 *
 * @author philwhiles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseQueryRequestDTO {

  private Boolean caseEvents = false;
}

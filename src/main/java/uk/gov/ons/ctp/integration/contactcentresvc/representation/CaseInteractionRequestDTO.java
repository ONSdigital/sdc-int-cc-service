package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType;

/** The request object when contact centre sends a Case Interaction */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInteractionRequestDTO {

  @NotNull private CaseInteractionType type;
  private CaseSubInteractionType subtype;
  private String note;
}

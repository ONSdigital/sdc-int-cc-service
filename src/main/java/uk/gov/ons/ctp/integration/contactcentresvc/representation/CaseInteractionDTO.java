package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The request object when contact centre sends a Case Interaction */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInteractionDTO {

  @NotNull String type;
  String subtype;
  String note;
}

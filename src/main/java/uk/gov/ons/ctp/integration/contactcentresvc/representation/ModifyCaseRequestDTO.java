package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre calls to modify the details for a case
 *
 * @author philwhiles
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyCaseRequestDTO extends CaseRequestDTO {

  @NotNull private UUID caseId;
}

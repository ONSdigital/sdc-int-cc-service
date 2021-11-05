package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre registers a refusal
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefusalRequestDTO {

  @NotNull private UUID caseId;

  @NotNull private Reason reason;

  @NotNull private Date dateTime;
}

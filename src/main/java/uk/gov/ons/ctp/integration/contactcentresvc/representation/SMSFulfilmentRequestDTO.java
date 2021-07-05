package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;
import uk.gov.ons.ctp.integration.contactcentresvc.Constants;

/**
 * The request object when contact centre requests an SMS fulfilment for a given case
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SMSFulfilmentRequestDTO {

  @NotNull private UUID caseId;

  @NotNull
  @Size(max = 20)
  @Pattern(regexp = Constants.UKMOBILEPHONENUMBER_RE)
  @LoggingScope(scope = Scope.MASK)
  private String telNo;

  @NotNull
  @Size(max = 12)
  private String fulfilmentCode;

  @NotNull private Date dateTime;
}

package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre requests a postal fulfilment for a given case
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostalFulfilmentRequestDTO {

  @NotNull private UUID caseId;

  @Size(max = 20)
  @LoggingScope(scope = Scope.SKIP)
  private String title;

  @Size(max = 35)
  @LoggingScope(scope = Scope.SKIP)
  private String forename;

  @Size(max = 35)
  @LoggingScope(scope = Scope.SKIP)
  private String surname;

  @NotNull private String fulfilmentCode;

  @NotNull private Date dateTime;
}

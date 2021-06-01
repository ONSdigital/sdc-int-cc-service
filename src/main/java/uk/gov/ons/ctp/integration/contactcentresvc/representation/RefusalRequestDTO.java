package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.Constants;

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

  @NotNull private Integer agentId;

  @Size(max = 12)
  @LoggingScope(scope = Scope.SKIP)
  private String title;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String forename;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String surname;

  @Size(max = 60)
  private String addressLine1;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String addressLine2;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String addressLine3;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String townName;

  @NotNull private Reason reason;

  private Region region;

  @Pattern(regexp = Constants.OPTIONAL_POSTCODE_RE)
  private String postcode;

  private UniquePropertyReferenceNumber uprn;

  @NotNull private Boolean isHouseholder;

  @NotNull private Date dateTime;

  private String callId;
}

package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.Constants;

/**
 * The request object when contact centre sends new case and address details
 *
 * @author philwhiles
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class NewCaseRequestDTO extends CaseRequestDTO {

  private UniquePropertyReferenceNumber uprn;

  @LoggingScope(scope = Scope.SKIP)
  @NotBlank
  @Size(max = 60)
  private String townName;

  @NotNull private Region region;

  @NotBlank
  @Pattern(regexp = Constants.POSTCODE_RE)
  private String postcode;
}

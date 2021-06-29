package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class CaseRequestDTO {

  @NotNull private CaseType caseType;
  @NotNull private EstabType estabType;

  @NotBlank
  @Size(max = 60)
  private String addressLine1;

  @LoggingScope(scope = Scope.MASK)
  @Size(max = 60)
  private String addressLine2;

  @LoggingScope(scope = Scope.MASK)
  @Size(max = 60)
  private String addressLine3;

  @NotNull private Date dateTime;

  @Size(max = 60)
  private String ceOrgName;

  private Integer ceUsualResidents;
}

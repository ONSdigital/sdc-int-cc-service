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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidateCaseRequestDTO {

  @NotNull private UUID caseId;

  @NotNull private CaseStatus status;

  @Size(max = 512)
  @LoggingScope(scope = Scope.SKIP)
  private String notes;

  @NotNull private Date dateTime;
}

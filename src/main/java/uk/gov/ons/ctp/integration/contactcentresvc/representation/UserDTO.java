package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
  @Email private String name;
  private boolean active;
  private List<String> userRoles;
  private List<String> adminRoles;
  private List<SurveyUsageDTO> surveyUsages;

  @Accessors(fluent = true)
  private boolean canBeDeleted;

}

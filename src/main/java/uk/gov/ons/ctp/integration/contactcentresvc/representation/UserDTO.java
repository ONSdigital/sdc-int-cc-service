package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private boolean isDeletable;
}

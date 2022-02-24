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
  @Email private String identity;
  private String forename;
  private String surname;
  private boolean active;
  private List<String> userRoles;
  private List<String> adminRoles;
  private List<SurveyUsageDTO> surveyUsages;
}

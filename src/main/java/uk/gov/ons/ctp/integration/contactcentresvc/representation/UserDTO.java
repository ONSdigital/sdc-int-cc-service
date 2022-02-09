package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
  private String name;
  private boolean active;
  private List<RoleDTO> userRoles;
  private List<RoleDTO> adminRoles;
  private List<SurveyUsageDTO> surveyUsages;
}

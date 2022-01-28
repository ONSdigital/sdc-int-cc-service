package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO extends BasicUserDTO {
  private List<RoleDTO> userRoles;
  private List<RoleDTO> adminRoles;
}

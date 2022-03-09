package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import lombok.Data;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;

@Data
public class RoleDTO {
  private String name;
  private String description;
  private List<PermissionType> permissions;
}

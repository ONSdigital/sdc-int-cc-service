package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class RoleDTO {
  private String name;
  private String description;
  List<PermissionDTO> permissions;
}

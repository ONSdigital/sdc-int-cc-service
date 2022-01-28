package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class RoleDTO {
  private UUID id;
  private String name;
  List<PermissionDTO> permissions;
}

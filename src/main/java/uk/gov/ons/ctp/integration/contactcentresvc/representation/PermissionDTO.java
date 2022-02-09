package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.Data;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;

@Data
public class PermissionDTO {
  private PermissionType permissionType;
}

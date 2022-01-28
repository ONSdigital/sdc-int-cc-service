package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.UUID;

import lombok.Data;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;

@Data
public class PermissionDTO {
  private PermissionType permissionType;
  private UUID surveyId;
}

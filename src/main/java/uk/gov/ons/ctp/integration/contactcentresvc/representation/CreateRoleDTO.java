package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.Data;

@Data
public class CreateRoleDTO {
  private String name;
  private String description;
}

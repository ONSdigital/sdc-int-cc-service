package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicUserDTO {
  private UUID id;
  private String name;
}

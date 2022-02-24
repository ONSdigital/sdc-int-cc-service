package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.UUID;
import lombok.Data;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;

@Data
public class DummyUserConfig {
  private boolean allowed;
  private String userIdentity;
  private UUID userId;

  public User getDummyUser() {
    return User.builder().id(userId).identity(userIdentity).build();
  }
}

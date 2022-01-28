package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class DummyUser {
  private boolean allowed;
  private String identity;
  private String superUserIdentity;
}

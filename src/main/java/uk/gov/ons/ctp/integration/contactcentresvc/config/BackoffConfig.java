package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class BackoffConfig {
  private int initial;
  private int multiplier;
  private int max;
}

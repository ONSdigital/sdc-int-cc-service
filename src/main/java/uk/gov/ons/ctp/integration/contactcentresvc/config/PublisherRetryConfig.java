package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class PublisherRetryConfig {
  private int initial;
  private String multiplier;
  private int max;
  private int maxAttempts;
}

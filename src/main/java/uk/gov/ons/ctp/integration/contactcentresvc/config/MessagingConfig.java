package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class MessagingConfig {

  private PublishConfig publish;
  private PublisherRetryConfig retry;

  @Data
  public static class PublishConfig {
    private int timeout;
  }
}

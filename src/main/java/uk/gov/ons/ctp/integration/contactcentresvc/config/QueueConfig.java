package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class QueueConfig {
  private String eventExchange;
  private String deadLetterExchange;
  private String caseQueue;
  private String caseQueueDLQ;
  private String caseRoutingKey;
}

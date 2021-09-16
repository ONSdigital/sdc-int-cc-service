package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class QueueConfig {
  private String caseSubscription;
  private String caseTopic;
  private String uacSubscription;
  private String uacTopic;
}

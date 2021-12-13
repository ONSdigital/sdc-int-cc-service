package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class QueueConfig {
  private String caseSubscription;
  private String surveySubscription;
  private String collectionExerciseSubscription;
  private String uacSubscription;
}

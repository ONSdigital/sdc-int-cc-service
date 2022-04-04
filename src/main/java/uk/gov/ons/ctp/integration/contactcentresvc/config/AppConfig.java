package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {

  private String surveyName;
  private AddressIndexSettings addressIndexSettings;
  private CaseServiceSettings caseServiceSettings;
  private Fulfilments fulfilments;
  private KeyStore keystore;
  private EqConfig eq;
  private Resource publicPgpKey1;
  private Resource publicPgpKey2;
  private UPRNBlacklist uprnBlacklist;
  private CustomCircuitBreakerConfig circuitBreaker;
  private QueueConfig queueConfig;
  private MessagingConfig messaging;
  private Set<String> surveys;
  private DummyUserConfig dummyUserConfig;
}

package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.RoleEndpointCaller;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.UserEndpointCaller;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendPoller;

/**
 * Base class for integration tests which need to span the entire service; from the endpoints down
 * to the database.
 *
 * <p>Note that this test uses the flyway profile, so the flyway scripts are applied to the database
 * when a test starts.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-containers-flyway")
@Testcontainers
@MockBean({EventToSendPoller.class, EventPublisher.class, PubSubTemplate.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "uacEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@Transactional
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
@Tag("db")
@Tag("fs")
public abstract class FullStackIntegrationTestBase {
  
  URL base;
  @LocalServerPort int port;
  
  RoleEndpointCaller roleEndpoint;
  UserEndpointCaller userEndpoint;

  public void init() throws MalformedURLException { 
    base = new URL("http://localhost:" + port);

    userEndpoint = new UserEndpointCaller(base);
    roleEndpoint = new RoleEndpointCaller(base);
  }
  
  public UserEndpointCaller userEndpoint() throws CTPException {
    if (userEndpoint == null) {
      throw new CTPException(Fault.SYSTEM_ERROR, "Test class has not initialised the test base");
    }
    
    return userEndpoint;
  }
  
  public RoleEndpointCaller roleEndpoint() throws CTPException {
    if (roleEndpoint == null) {
      throw new CTPException(Fault.SYSTEM_ERROR, "Test class has not initialised the test base");
    }
    
    return roleEndpoint;
  }
}

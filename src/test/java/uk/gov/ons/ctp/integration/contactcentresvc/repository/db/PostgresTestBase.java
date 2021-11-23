package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendPoller;

/** Base class for spring tests that run with TestContainer to assist with database testing. */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test-containers-flyway")
@Testcontainers
@TestPropertySource(properties = {"GOOGLE_CLOUD_PROJECT=sdc-cc-test"})
@MockBean({EventToSendPoller.class, EventPublisher.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@Transactional
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public abstract class PostgresTestBase {}
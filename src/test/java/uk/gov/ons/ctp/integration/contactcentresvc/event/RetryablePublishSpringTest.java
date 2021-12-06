package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventPublishException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendProcessor.PublishRetrier;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

@EnableRetry
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PublishRetrier.class, PublishRetryListener.class, AppConfig.class})
@TestPropertySource(
    properties = {
      "messaging.retry.initial=10",
      "messaging.retry.multiplier=1.2",
      "messaging.retry.max=300",
      "messaging.retry.max-attempts=3",
    })
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class RetryablePublishSpringTest {
  private static final String ID_1 = "9bd616a8-147c-11ec-9bec-4c3275913db5";

  @MockBean private EventPublisher eventPublisher;

  @Autowired private PublishRetrier retrier;

  @Captor private ArgumentCaptor<TopicType> typeCaptor;
  @Captor private ArgumentCaptor<Source> sourceCaptor;
  @Captor private ArgumentCaptor<Channel> channelCaptor;
  @Captor private ArgumentCaptor<String> payloadCaptor;

  @Test
  public void shouldAutowire() {
    assertNotNull(retrier);
  }

  private EventToSend createEvent(String id) {
    String payload = FixtureHelper.loadPackageObjectNode("EqLaunch").toString();
    return new EventToSend(
        UUID.fromString(id), TopicType.EQ_LAUNCH.name(), payload, LocalDateTime.now());
  }

  private void verifySent(int numTimes) {
    verify(eventPublisher, times(numTimes))
        .sendEvent(
            typeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            payloadCaptor.capture());

    if (numTimes != 0) {
      assertEquals(TopicType.EQ_LAUNCH, typeCaptor.getValue());
      assertEquals(Source.CONTACT_CENTRE_API, sourceCaptor.getValue());
      assertEquals(Channel.CC, channelCaptor.getValue());
      assertNotNull(payloadCaptor.getValue());
    }
  }

  @Test
  public void shouldPublish() throws Exception {
    when(eventPublisher.sendEvent(any(), any(), any(), anyString()))
        .thenReturn(UUID.fromString(ID_1));

    EventToSend event = createEvent(ID_1);
    retrier.publish(event);

    verifySent(1);
  }

  @Test
  public void shouldRetryPublishOnce() throws Exception {
    doThrow(new EventPublishException("argh"))
        .doReturn(UUID.fromString(ID_1))
        .when(eventPublisher)
        .sendEvent(any(), any(), any(), anyString());

    EventToSend event = createEvent(ID_1);
    retrier.publish(event);

    verifySent(2);
  }

  @Test
  public void shouldRetryPublishTwice() throws Exception {
    doThrow(new EventPublishException("argh"))
        .doThrow(new EventPublishException("not again!"))
        .doReturn(UUID.fromString(ID_1))
        .when(eventPublisher)
        .sendEvent(any(), any(), any(), anyString());

    EventToSend event = createEvent(ID_1);
    retrier.publish(event);

    verifySent(3);
  }

  @Test
  public void shouldRetryPublishTillExhaustion() throws Exception {
    doThrow(new EventPublishException("argh"))
        .when(eventPublisher)
        .sendEvent(any(), any(), any(), anyString());

    try {
      EventToSend event = createEvent(ID_1);
      retrier.publish(event);
      fail();
    } catch (EventPublishException e) {
      assertEquals("argh", e.getMessage());
    }
    verifySent(3);
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@ExtendWith(MockitoExtension.class)
public class EventToSendProcessorTest {
  private static final String ID_1 = "9bd616a8-147c-11ec-9bec-4c3275913db5";
  private static final String ID_2 = "a4aa361c-1498-11ec-8d74-4c3275913db5";
  private static final String ID_3 = "ac0c49fe-1498-11ec-879a-4c3275913db5";

  @Mock private EventToSendRepository eventToSendRepository;
  @Mock private EventPublisher eventPublisher;
  @InjectMocks private EventToSendProcessor processor;

  @Captor private ArgumentCaptor<Integer> chunkSizeCaptor;
  @Captor private ArgumentCaptor<TopicType> typeCaptor;
  @Captor private ArgumentCaptor<Source> sourceCaptor;
  @Captor private ArgumentCaptor<Channel> channelCaptor;
  @Captor private ArgumentCaptor<String> payloadCaptor;
  @Captor private ArgumentCaptor<Iterable<EventToSend>> sentCaptor;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(processor, "chunkSize", 3);
  }

  private EventToSend createEvent(String id) {
    String payload = FixtureHelper.loadPackageObjectNode("SurveyLaunchResponse").toString();
    return new EventToSend(
        UUID.fromString(id), TopicType.SURVEY_LAUNCH.name(), payload, LocalDateTime.now());
  }

  @Test
  public void shouldProcessChunksOfChunkSize() {
    processor.processChunk();
    verify(eventToSendRepository).findEventsToSend(chunkSizeCaptor.capture());
    assertEquals(Integer.valueOf(3), chunkSizeCaptor.getValue());
  }

  @Test
  public void shouldProcessNothing() {
    when(eventToSendRepository.findEventsToSend(3)).thenReturn(Stream.empty());
    int numProcessed = processor.processChunk();
    assertEquals(0, numProcessed);
    verify(eventToSendRepository).findEventsToSend(3);
  }

  private void validatePayload(int index) {
    String payload = payloadCaptor.getAllValues().get(index);
    assertTrue(payload.contains("\"1358980545\"")); // fixture QID
  }

  @Test
  public void shouldProcessOneItem() {
    List<EventToSend> events = new ArrayList<>();
    EventToSend ev = createEvent(ID_1);
    events.add(ev);
    when(eventToSendRepository.findEventsToSend(anyInt())).thenReturn(events.stream());
    int numProcessed = processor.processChunk();
    assertEquals(1, numProcessed);
    verify(eventPublisher)
        .sendEvent(
            typeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            payloadCaptor.capture());

    assertEquals(TopicType.SURVEY_LAUNCH, typeCaptor.getValue());
    assertEquals(Source.CONTACT_CENTRE_API, sourceCaptor.getValue());
    assertEquals(Channel.CC, channelCaptor.getValue());
    validatePayload(0);

    verify(eventToSendRepository).deleteAllInBatch(sentCaptor.capture());
    assertEquals(1, ((List<EventToSend>) sentCaptor.getValue()).size());
  }

  @Test
  public void shouldProcessMultipleItems() {
    List<EventToSend> events = new ArrayList<>();
    EventToSend ev1 = createEvent(ID_1);
    EventToSend ev2 = createEvent(ID_2);
    EventToSend ev3 = createEvent(ID_3);
    events.add(ev1);
    events.add(ev2);
    events.add(ev3);
    when(eventToSendRepository.findEventsToSend(anyInt())).thenReturn(events.stream());
    int numProcessed = processor.processChunk();
    assertEquals(3, numProcessed);
    verify(eventPublisher, times(3))
        .sendEvent(
            typeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            payloadCaptor.capture());

    for (int i = 0; i < 3; i++) {
      assertEquals(TopicType.SURVEY_LAUNCH, typeCaptor.getAllValues().get(i));
      assertEquals(Source.CONTACT_CENTRE_API, sourceCaptor.getAllValues().get(i));
      assertEquals(Channel.CC, channelCaptor.getAllValues().get(i));
      validatePayload(i);
    }

    verify(eventToSendRepository).deleteAllInBatch(sentCaptor.capture());
    assertEquals(3, ((List<EventToSend>) sentCaptor.getValue()).size());
  }
}

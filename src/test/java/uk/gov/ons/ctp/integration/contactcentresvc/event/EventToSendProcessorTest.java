package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@ExtendWith(MockitoExtension.class)
public class EventToSendProcessorTest {
  private static final String ID_1 = "9bd616a8-147c-11ec-9bec-4c3275913db5";
  private static final String ID_2 = "a4aa361c-1498-11ec-8d74-4c3275913db5";
  private static final String ID_3 = "ac0c49fe-1498-11ec-879a-4c3275913db5";

  @Mock private EventToSendRepository eventToSendRepository;
  @Mock private EventPublisher eventPublisher;
  @Mock private AppConfig appConfig;
  @InjectMocks private EventToSendProcessor processor;

  @Captor private ArgumentCaptor<Integer> chunkSizeCaptor;
  @Captor private ArgumentCaptor<EventType> typeCaptor;
  @Captor private ArgumentCaptor<Source> sourceCaptor;
  @Captor private ArgumentCaptor<Channel> channelCaptor;
  @Captor private ArgumentCaptor<EventPayload> payloadCaptor;
  @Captor private ArgumentCaptor<Iterable<EventToSend>> sentCaptor;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(processor, "chunkSize", 3);
    lenient().when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  private EventToSend createEvent(String id) {
    String payload = FixtureHelper.loadPackageObjectNode("SurveyLaunchResponse").toString();
    return new EventToSend(UUID.fromString(id), EventType.SURVEY_LAUNCH.name(), payload);
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
    processor.processChunk();
    verify(eventToSendRepository).findEventsToSend(3);
  }

  @Test
  public void shouldOmitBrokenJsonPayload() {
    List<EventToSend> events = new ArrayList<>();
    EventToSend ev = createEvent(ID_1);
    ev.setPayload("not json");
    events.add(ev);
    when(eventToSendRepository.findEventsToSend(anyInt())).thenReturn(events.stream());
    processor.processChunk();
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
    verify(eventToSendRepository).deleteAllInBatch(sentCaptor.capture());
    assertTrue(((List<EventToSend>) sentCaptor.getValue()).isEmpty());
  }

  @Test
  public void shouldProcessOneItem() {
    List<EventToSend> events = new ArrayList<>();
    EventToSend ev = createEvent(ID_1);
    events.add(ev);
    when(eventToSendRepository.findEventsToSend(anyInt())).thenReturn(events.stream());
    processor.processChunk();
    verify(eventPublisher)
        .sendEvent(
            typeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            payloadCaptor.capture());

    assertEquals(EventType.SURVEY_LAUNCH, typeCaptor.getValue());
    assertEquals(Source.CONTACT_CENTRE_API, sourceCaptor.getValue());
    assertEquals(Channel.CC, channelCaptor.getValue());

    EventPayload payload = payloadCaptor.getValue();
    assertTrue(payload instanceof SurveyLaunchResponse);

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
    processor.processChunk();
    verify(eventPublisher, times(3))
        .sendEvent(
            typeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            payloadCaptor.capture());

    for (int i = 0; i < 3; i++) {
      assertEquals(EventType.SURVEY_LAUNCH, typeCaptor.getAllValues().get(i));
      assertEquals(Source.CONTACT_CENTRE_API, sourceCaptor.getAllValues().get(i));
      assertEquals(Channel.CC, channelCaptor.getAllValues().get(i));

      EventPayload payload = payloadCaptor.getAllValues().get(i);
      assertTrue(payload instanceof SurveyLaunchResponse);
    }

    verify(eventToSendRepository).deleteAllInBatch(sentCaptor.capture());
    assertEquals(3, ((List<EventToSend>) sentCaptor.getValue()).size());
  }

  @Test
  public void noWorkToDo() {
    when(eventToSendRepository.count()).thenReturn(0L);
    assertFalse(processor.isThereWorkToDo());
  }

  @Test
  public void someWorkToDo() {
    when(eventToSendRepository.count()).thenReturn(4L);
    assertTrue(processor.isThereWorkToDo());
  }
}

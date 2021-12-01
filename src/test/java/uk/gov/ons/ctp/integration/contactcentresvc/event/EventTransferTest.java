package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@ExtendWith(MockitoExtension.class)
public class EventTransferTest {
  @Mock private EventToSendRepository eventToSendRepository;
  @Spy private CustomObjectMapper mapper = new CustomObjectMapper();

  @InjectMocks private EventTransfer eventTransfer;

  @Captor private ArgumentCaptor<EventToSend> eventCaptor;

  private EventPayload createPayload() {
    return EqLaunch.builder().qid("123").build();
  }

  @Test
  public void shouldSend() {
    EventPayload payload = createPayload();
    UUID transferId = eventTransfer.send(TopicType.EQ_LAUNCH, payload);
    assertNotNull(transferId);
    verify(eventToSendRepository).save(eventCaptor.capture());
    EventToSend event = eventCaptor.getValue();
    String payloadJson = event.getPayload();
    assertEquals(TopicType.EQ_LAUNCH.name(), event.getType());
    assertTrue(payloadJson.contains("\"123\""));
    assertNotNull(event.getId());
  }
}

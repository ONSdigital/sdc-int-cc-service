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
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@ExtendWith(MockitoExtension.class)
public class EventTransferTest {
  private static final String CASE_ID = "9bd616a8-147c-11ec-9bec-4c3275913db5";

  @Mock private EventToSendRepository eventToSendRepository;
  @Spy private CustomObjectMapper mapper = new CustomObjectMapper();

  @InjectMocks private EventTransfer eventTransfer;

  @Captor private ArgumentCaptor<EventToSend> eventCaptor;

  private EventPayload createPayload() {
    return SurveyLaunchResponse.builder()
        .questionnaireId("123")
        .caseId(UUID.fromString(CASE_ID))
        .agentId("456")
        .build();
  }

  @Test
  public void shouldSend() {
    EventPayload payload = createPayload();
    UUID transferId = eventTransfer.send(EventType.SURVEY_LAUNCH, payload);
    assertNotNull(transferId);
    verify(eventToSendRepository).save(eventCaptor.capture());
    EventToSend event = eventCaptor.getValue();
    String payloadJson = event.getPayload();
    assertEquals(EventType.SURVEY_LAUNCH.name(), event.getType());
    assertTrue(payloadJson.contains("\"" + CASE_ID + "\""));
    assertTrue(payloadJson.contains("\"123\""));
    assertTrue(payloadJson.contains("\"456\""));
    assertNotNull(event.getId());
  }
}

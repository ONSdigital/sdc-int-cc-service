package uk.gov.ons.ctp.integration.contactcentresvc.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.EventToSendRepository;

@Component
public class EventTransfer {
  private final EventToSendRepository eventToSendRepository;
  private final CustomObjectMapper mapper;

  public EventTransfer(EventToSendRepository messageToSendRepository, CustomObjectMapper mapper) {
    this.eventToSendRepository = messageToSendRepository;
    this.mapper = mapper;
  }

  public UUID send(EventType type, EventPayload payload) {
    EventToSend event = new EventToSend();
    event.setId(UUID.randomUUID());
    event.setType(type.name());
    event.setPayload(convertObjectToJson(payload));

    eventToSendRepository.save(event);
    return event.getId();
  }

  private String convertObjectToJson(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }
}

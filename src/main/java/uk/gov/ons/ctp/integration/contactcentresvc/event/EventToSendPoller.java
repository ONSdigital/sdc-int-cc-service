package uk.gov.ons.ctp.integration.contactcentresvc.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventToSendPoller {
  private final EventToSendProcessor processor;

  @Value("${scheduler.trace:false}")
  private boolean trace;

  public EventToSendPoller(EventToSendProcessor messageToSendProcessor) {
    this.processor = messageToSendProcessor;
  }

  @Scheduled(fixedDelayString = "${scheduler.fixed-delay-millis}")
  public void processQueuedMessages() {
    if (trace) {
      log.debug("processing events");
    }
    do {
      processor.processChunk();
    } while (processor.isThereWorkToDo());
  }
}

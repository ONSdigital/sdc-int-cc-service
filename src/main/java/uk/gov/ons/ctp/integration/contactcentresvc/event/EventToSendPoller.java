package uk.gov.ons.ctp.integration.contactcentresvc.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventToSendPoller {
  private final EventToSendProcessor processor;

  public EventToSendPoller(EventToSendProcessor messageToSendProcessor) {
    this.processor = messageToSendProcessor;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void processQueuedMessages() {
    do {
      processor.processChunk();
    } while (processor.isThereWorkToDo()); // No sleep while there's work to do!
  }
}

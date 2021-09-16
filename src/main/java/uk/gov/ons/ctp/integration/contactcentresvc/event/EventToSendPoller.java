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

  /**
   * Process chunks of queued messages, until we can no longer detect any waiting to be processed.
   *
   * <p>This is a scheduled job which will run with a fixed delay between each invocation.
   */
  @Scheduled(fixedDelayString = "${scheduler.fixed-delay-millis}")
  public void processQueuedMessages() {
    if (trace) {
      log.debug("processing events");
    }
    long numProcessed;
    do {
      numProcessed = processor.processChunk();
      if (trace) {
        log.debug("processed {} events", numProcessed);
      }
    } while (numProcessed > 0);
  }
}

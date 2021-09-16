package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventToSendPollerTest {

  @Mock private EventToSendProcessor processor;

  @InjectMocks private EventToSendPoller poller;

  @Test
  public void shouldProcessOneChunk() {
    when(processor.processChunk()).thenReturn(0);
    poller.processQueuedMessages();
    verify(processor).processChunk();
  }

  @Test
  public void shouldProcessTwoChunks() {
    when(processor.processChunk()).thenReturn(3).thenReturn(0);
    poller.processQueuedMessages();
    verify(processor, times(2)).processChunk();
  }
}

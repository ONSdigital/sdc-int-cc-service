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
    when(processor.isThereWorkToDo()).thenReturn(false);
    poller.processQueuedMessages();
    verify(processor).processChunk();
    verify(processor).isThereWorkToDo();
  }

  @Test
  public void shouldProcessTwoChunks() {
    when(processor.isThereWorkToDo()).thenReturn(true).thenReturn(false);
    poller.processQueuedMessages();
    verify(processor, times(2)).processChunk();
    verify(processor, times(2)).isThereWorkToDo();
  }
}

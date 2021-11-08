package uk.gov.ons.ctp.integration.contactcentresvc.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PublishRetryListener extends RetryListenerSupport {

  @Override
  public <T, E extends Throwable> void onError(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    Object operationName = context.getAttribute(RetryContext.NAME);
    if (log.isDebugEnabled()) {
      log.debug("{}: failed ; retried {} times ", operationName, context.getRetryCount());
    }
  }

  @Override
  public <T, E extends Throwable> void close(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

    // Spring retries have completed. Report on outcome if retries have been used.
    if (context.getRetryCount() > 0) {
      Object operationName = context.getAttribute(RetryContext.NAME);

      if (throwable == null) {
        int numAttempts = context.getRetryCount() + 1; // Add 1 to count the initial attempt
        log.info("{}: Publish successful after {} attempts", operationName, numAttempts);

      } else {
        // On failure the retryCount actually holds the number of attempts
        int numAttempts = context.getRetryCount();
        log.warn("{}: Publish failed after {} attempts", operationName, numAttempts);
      }
    }
  }
}

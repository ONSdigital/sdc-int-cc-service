package uk.gov.ons.ctp.integration.contactcentresvc;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CCsvcWebFilter implements Filter {
  private static final String USER_HEADER = "x-user-id";
  private static final String USER_KEY = "user"; // this will be referenced by logback config

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      HttpServletRequest req = (HttpServletRequest) request;
      String user = req.getHeader(USER_HEADER);
      if (user != null) {
        UserContext.set(user);
        MDC.put(USER_KEY, user);
      }
      chain.doFilter(request, response);
    } finally {
      UserContext.clear();
      MDC.remove(USER_KEY);
    }
  }
}

package uk.gov.ons.ctp.integration.contactcentresvc.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;

public class UserIdentityInterceptor implements HandlerInterceptor {
  private static final String USER_HEADER = "x-user-id";
  private static final String PRINCIPAL_KEY = "principal";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String userName = request.getHeader(USER_HEADER);

    UserIdentityContext.set(userName);
    MDC.put(PRINCIPAL_KEY, userName);
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    UserIdentityContext.clear();
    MDC.remove(PRINCIPAL_KEY);
  }
}

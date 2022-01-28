package uk.gov.ons.ctp.integration.contactcentresvc.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

public class UserIdentityInterceptor implements HandlerInterceptor {
  private final UserIdentityHelper userIdentity;

  public UserIdentityInterceptor(UserIdentityHelper userIdentity) {
    this.userIdentity = userIdentity;
  }

  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    //    if (request.getRequestURI().equals("/api/upload")) {
    //      // Don't bother with identity for file uploads because the JWT claim expires after 10
    // mins
    //      return true;
    //    }
    String userName = request.getHeader("x-user-id");
    if (userName == null) {
      throw new CTPException(Fault.ACCESS_DENIED);
    }
    request.setAttribute("principal", userName);
    return true;
  }
}

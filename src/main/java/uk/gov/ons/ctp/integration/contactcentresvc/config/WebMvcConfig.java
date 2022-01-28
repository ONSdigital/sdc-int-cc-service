package uk.gov.ons.ctp.integration.contactcentresvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final UserIdentityHelper userIdentity;

  public WebMvcConfig(UserIdentityHelper userIdentity) {
    this.userIdentity = userIdentity;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new UserIdentityInterceptor(userIdentity));
  }
}

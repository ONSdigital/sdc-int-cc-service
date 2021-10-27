package uk.gov.ons.ctp.integration.contactcentresvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.ons.ctp.common.domain.Channel;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${spring.security.user.name}")
  String username;

  @Value("${spring.security.user.password}")
  String password;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // THE ORDER OF THE MATCHERS BELOW IS IMPORTANT
    http.authorizeRequests()
        .antMatchers("/info")
        .permitAll()
        .antMatchers("/version")
        .permitAll()
        .antMatchers("/cases/**/uac")
        .hasRole("CC")
        .antMatchers("/cases")
        .hasRole("CC")
        .antMatchers("/cases/uprn/**")
        .hasRole("CC")
        .antMatchers("/addresses")
        .hasRole("CC")
        .antMatchers("/addresses/postcode")
        .hasRole("CC")
        .antMatchers("/fulfilments/**")
        .hasRole("CC")
        .antMatchers("/cases/**") // AND LASTLY ANY remaining /cases paths
        .hasRole("CC")
        .and()
        .csrf()
        .disable()
        .httpBasic();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
        .withUser(username)
        .password("{noop}" + password)
        .roles(String.valueOf(Channel.CC));
  }
}

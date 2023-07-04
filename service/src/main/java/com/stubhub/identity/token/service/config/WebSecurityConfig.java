package com.stubhub.identity.token.service.config;

import com.google.common.base.Strings;
import com.stubhub.identity.token.service.auth.IdentityAuthenticationProvider;
import com.stubhub.identity.token.service.client.CustomBCryptPasswordEncoder;
import com.stubhub.identity.token.service.session.ShapeSessionService;
import java.util.Arrays;
import javax.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Order(1)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${server.servlet.session.cookie.legacy}")
  private String legacySessionName;

  @Value("${server.servlet.session.cookie.name}")
  private String tokenSessionName;

  @Autowired private IdentityAuthenticationProvider identityAuthenticationProvider;

  @Autowired private ShapeSessionService shapeSessionService;

  @Value("${mode.test}")
  private Boolean isTest;

  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new CustomBCryptPasswordEncoder(4);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.requestMatchers()
        .antMatchers(
            "/",
            "/login",
            "/oauth/root",
            "/oauth/logout**",
            "/oauth/login**",
            "/oauth/v1/token",
            "/oauth/v1/authorize",
            "/oauth/v1/check_token",
            "/oauth/v1/ext/token/revoke",
            "/oauth/v1/.well-known/**")
        .and()
        .authorizeRequests()
        .antMatchers("/oauth/v1/authorize")
        .authenticated()
        .and()
        .authorizeRequests()
        .antMatchers(
            "/actuator/**",
            "/oauth/logout**",
            "/oauth/login**",
            "/oauth/v1/token",
            "/oauth/v1/check_token",
            "/oauth/v1/ext/token/revoke",
            "/oauth/v1/.well-known/**")
        .permitAll()
        .and()
        .logout()
        .logoutUrl("/oauth/logout")
        .addLogoutHandler(
            (request, response, authentication) -> {
              Cookie cookie = new Cookie(legacySessionName, null);
              String cookiePath = "/";
              cookie.setPath(cookiePath);
              cookie.setMaxAge(0);
              response.addCookie(cookie);
            })
        .deleteCookies(tokenSessionName)
        .logoutSuccessHandler(
            (httpServletRequest, httpServletResponse, authentication) -> {
              shapeSessionService.invalidateSession(httpServletRequest);
              String redirect_uri = httpServletRequest.getParameter("redirect_uri");
              if (!Strings.isNullOrEmpty(redirect_uri)) {
                httpServletResponse.sendRedirect(redirect_uri);
              }
            })
        .permitAll();

    if (isTest) {
      http.formLogin().permitAll();
    } else {
      http.formLogin().loginPage("/oauth/login").permitAll();
    }

    http.cors()
        .and()
        .csrf()
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .disable();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(identityAuthenticationProvider);
  }

  @Configuration
  @EnableResourceServer
  protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(HttpSecurity http) throws Exception {
      http.cors()
          .and()
          .authorizeRequests()
          .antMatchers("/oauth/v1/user/me", "/oauth/v1/userinfo")
          .authenticated();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
      resources.resourceId("api://stubhub");
    }
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("POST", "GET", "DELETE", "OPTIONS"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

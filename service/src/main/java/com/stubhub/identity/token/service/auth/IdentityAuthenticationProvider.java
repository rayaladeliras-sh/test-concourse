package com.stubhub.identity.token.service.auth;

import com.stubhub.identity.token.service.user.IdentityUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IdentityAuthenticationProvider implements AuthenticationProvider {

  @Autowired private IdentityUserDetailsService identityUserDetailsService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    try {
      return identityUserDetailsService.authenticate(
          authentication.getPrincipal().toString(), authentication.getCredentials().toString());
    } catch (Exception e) {
      log.error(
          "class=IdentityAuthenticationProvider authentication error: {}, stack: {}",
          e.getLocalizedMessage(),
          e.getStackTrace());
      throw e;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }
}

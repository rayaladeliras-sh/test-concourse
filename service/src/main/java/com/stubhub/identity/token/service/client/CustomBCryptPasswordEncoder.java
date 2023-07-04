package com.stubhub.identity.token.service.client;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;

public class CustomBCryptPasswordEncoder extends BCryptPasswordEncoder {

  public CustomBCryptPasswordEncoder(int strength) {
    super(strength);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    boolean match = super.matches(rawPassword, encodedPassword);
    // for refresh token with opaque token
    if (match) {
      RequestContextHolder.currentRequestAttributes()
          .setAttribute("clientSecret", rawPassword, SCOPE_REQUEST);
    }
    return match;
  }
}

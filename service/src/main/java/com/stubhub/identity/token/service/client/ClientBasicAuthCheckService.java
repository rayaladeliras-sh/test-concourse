package com.stubhub.identity.token.service.client;

import java.io.IOException;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientBasicAuthCheckService {
  @Autowired private IdentityClientDetailsService identityClientDetailsService;

  @Autowired private PasswordEncoder passwordEncoder;

  // copy from spring
  private String[] extractAndDecodeHeader(String header) throws IOException {
    byte[] base64Token = header.substring(6).getBytes("UTF-8");

    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(base64Token);
    } catch (IllegalArgumentException var7) {
      throw new BadCredentialsException("Failed to decode basic authentication token");
    }

    String token = new String(decoded, "UTF-8");
    int delim = token.indexOf(":");
    if (delim == -1) {
      throw new BadCredentialsException("Invalid basic authentication token");
    } else {
      return new String[] {token.substring(0, delim), token.substring(delim + 1)};
    }
  }

  public OAuthClientDetails checkClientCredentials(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.toLowerCase().startsWith("basic ")) {
      try {
        String[] tokens = this.extractAndDecodeHeader(header);
        assert tokens.length == 2;
        String username = tokens[0];
        try {
          OAuthClientDetails clientDetails =
              identityClientDetailsService.loadClientByClientId(username);
          if (passwordEncoder.matches(tokens[1], clientDetails.getClientSecret())) {
            return clientDetails;
          }
        } catch (ClientRegistrationException ex) {
          log.error("method=checkClientCredentials load client fail {}", ex.getLocalizedMessage());
        }
      } catch (IOException e) {
        log.error("method=checkClientCredential parse header fail {}", e.getLocalizedMessage());
      }
    }
    throw new BadCredentialsException(
        "The client id or client secret supplied for a confidential client is invalid.");
  }
}

package com.stubhub.identity.token.service.token.converter;

import com.stubhub.identity.token.service.token.OAuthConstants;
import com.stubhub.identity.token.service.user.IdentityUserDetails;
import com.stubhub.identity.token.service.user.IdentityUserDetailsService;
import com.stubhub.identity.token.service.user.OAuthUserDetails;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

  @Autowired private IdentityUserDetailsService identityUserDetailsService;

  @Autowired
  public CustomUserAuthenticationConverter(IdentityUserDetailsService userDetailsService) {
    this.setUserDetailsService(userDetailsService);
  }

  @Override
  public Map<String, ?> convertUserAuthentication(Authentication userAuthentication) {
    Map<String, Object> response = new LinkedHashMap<>();
    String username;
    if (userAuthentication.getPrincipal() instanceof OAuthUserDetails) {
      username = ((OAuthUserDetails) userAuthentication.getPrincipal()).getUsername();
    } else if (userAuthentication.getPrincipal() instanceof String) {
      username = ((String) userAuthentication.getPrincipal());
    } else {
      log.error("can't get user name from authentication");
      throw new UsernameNotFoundException("can't get user name from authentication");
    }
    IdentityUserDetails identityUserDetails = identityUserDetailsService.findUserByEmail(username);
    response.putAll(super.convertUserAuthentication(userAuthentication));
    response.put(OAuthConstants.CLAIM_SUB, identityUserDetails.getGuid());
    response.put(OAuthConstants.CLAIM_USER_NAME, identityUserDetails.getEmail());

    return response;
  }
}

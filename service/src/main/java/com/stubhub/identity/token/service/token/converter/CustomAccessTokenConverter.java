package com.stubhub.identity.token.service.token.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.service.token.OAuthConstants;
import com.stubhub.identity.token.service.token.act.ActClaimDto;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class CustomAccessTokenConverter extends DefaultAccessTokenConverter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  public CustomAccessTokenConverter(
      CustomUserAuthenticationConverter customUserAuthenticationConverter) {
    this.setUserTokenConverter(customUserAuthenticationConverter);
  }

  @Override
  public Map<String, ?> convertAccessToken(
      OAuth2AccessToken token, OAuth2Authentication authentication) {
    Map<String, Object> data =
        (Map<String, Object>) super.convertAccessToken(token, authentication);

    // add basic claims for both app and user token
    data.putIfAbsent(AccessTokenConverter.AUD, new HashSet<>());
    ((Set<String>) data.get(AccessTokenConverter.AUD))
        .add(OAuthConstants.DEFAULT_CLAIM_AUDIENCE_VALUE);
    data.put(OAuthConstants.CLAIM_ISSUED_AT, System.currentTimeMillis() / 1000);
    data.put(OAuthConstants.CLAIM_ISSUE_URL, OAuthConstants.DEFAULT_CLAIM_ISSUE_URL_VALUE);

    if (null == data.get(OAuthConstants.CLAIM_SUB)) {
      data.put(OAuthConstants.CLAIM_SUB, data.get(OAuthConstants.CLAIM_CLIENT_ID));
    }

    if (isACTSupport(authentication.getOAuth2Request())) {
      String actClaimDto =
          authentication.getOAuth2Request().getRequestParameters().get(OAuthConstants.CLAIM_ACT);
      try {
        log.info("method=convertAccessToken act information: {}", actClaimDto);
        ActClaimDto claimDto = objectMapper.readValue(actClaimDto, ActClaimDto.class);
        data.put(OAuthConstants.CLAIM_ACT, claimDto);
      } catch (IOException e) {
        log.error("method=convertAccessToken read act object fail: {}", e.getLocalizedMessage());
      }
    }
    return data;
  }

  private boolean isACTSupport(OAuth2Request oAuth2Request) {
    Predicate<OAuth2Request> actRequestPredicate =
        request ->
            OAuthConstants.GRANT_TYPE_PASSWORD.equalsIgnoreCase(request.getGrantType())
                && !StringUtils.isEmpty(
                    request.getRequestParameters().get(OAuthConstants.CLAIM_ACT));
    return actRequestPredicate.test(oAuth2Request);
  }
}

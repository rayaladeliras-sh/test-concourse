package com.stubhub.identity.token.service.token.refresh;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import com.stubhub.identity.token.service.exception.ExtendedErrorException;
import com.stubhub.identity.token.service.token.shape.ShapeTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.refresh.RefreshTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Service
public class CustomRefreshTokenGranter extends RefreshTokenGranter {

  @Autowired private ShapeTokenService shapeTokenService;
  @Autowired private OAuthRefreshTokenService oAuthRefreshTokenService;

  @Lazy
  public CustomRefreshTokenGranter(
      @Qualifier("defaultTokenServices") AuthorizationServerTokenServices tokenServices,
      ClientDetailsService clientDetailsService,
      OAuth2RequestFactory requestFactory) {
    super(tokenServices, clientDetailsService, requestFactory);
  }

  @Override
  protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest) {
    String refreshToken = tokenRequest.getRequestParameters().get("refresh_token");

    if (oAuthRefreshTokenService.isExistInDB(refreshToken)) {
      log.info("method=getAccessToken refresh token by cloud service param={}", refreshToken);
      return getTokenServices().refreshAccessToken(refreshToken, tokenRequest);
    } else {
      log.info("method=getAccessToken start refresh token by shape service param={}", refreshToken);
      try {
        String clientSecret =
            RequestContextHolder.currentRequestAttributes()
                .getAttribute("clientSecret", SCOPE_REQUEST)
                .toString();
        OAuth2AccessToken token =
            shapeTokenService.refreshJwtToken(client.getClientId(), clientSecret, refreshToken);
        log.info("method=getAccessToken refresh token by shape service successfully");
        return token;
      } catch (ExtendedErrorException ex) {
        log.error(
            "method=getAccessToken refresh token {} from shape service fail with status={} {}",
            refreshToken,
            ex.getStatus(),
            ex.getReason());
        throw new AuthenticationServiceException(ex.getReason(), ex);
      }
    }
  }
}

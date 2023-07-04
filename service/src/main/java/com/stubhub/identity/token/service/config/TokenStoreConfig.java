package com.stubhub.identity.token.service.config;

import com.stubhub.identity.token.service.auditlog.AuditLogService;
import com.stubhub.identity.token.service.token.CustomJwtTokenStore;
import com.stubhub.identity.token.service.token.converter.CustomAccessTokenConverter;
import com.stubhub.identity.token.service.token.converter.CustomJwtAccessTokenConverter;
import com.stubhub.identity.token.service.token.refresh.OAuthRefreshTokenService;
import java.security.KeyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

@Configuration
public class TokenStoreConfig {

  @Autowired private KeyPair keyPair;
  @Autowired private CustomAccessTokenConverter customAccessTokenConverter;
  @Autowired private OAuthRefreshTokenService oAuthRefreshTokenService;
  @Autowired private AuditLogService auditLogService;

  @Bean
  public JwtAccessTokenConverter jwtAccessTokenConverter() {
    JwtAccessTokenConverter converter = new CustomJwtAccessTokenConverter();
    converter.setKeyPair(keyPair);
    converter.setAccessTokenConverter(customAccessTokenConverter);
    return converter;
  }

  @Bean
  public TokenStore tokenStore() {
    return new CustomJwtTokenStore(jwtAccessTokenConverter(), oAuthRefreshTokenService, auditLogService);
  }
}

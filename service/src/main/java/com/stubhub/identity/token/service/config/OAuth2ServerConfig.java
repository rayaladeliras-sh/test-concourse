package com.stubhub.identity.token.service.config;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import com.stubhub.identity.token.service.client.IdentityClientDetailsService;
import com.stubhub.identity.token.service.token.code.SpannerAuthorizationCodeServices;
import com.stubhub.identity.token.service.token.refresh.CustomRefreshTokenGranter;
import com.stubhub.identity.token.service.user.IdentityUserDetailsService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenGranter;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeTokenGranter;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Configuration
@EnableAuthorizationServer
public class OAuth2ServerConfig extends AuthorizationServerConfigurerAdapter {

  @Autowired
  @Qualifier("authenticationManagerBean")
  private AuthenticationManager authenticationManager;

  @Autowired private JwtAccessTokenConverter jwtAccessTokenConverter;

  @Autowired private IdentityUserDetailsService identityUserDetailsService;

  @Autowired private TokenStore tokenStore;

  @Autowired private IdentityClientDetailsService identityClientDetailsService;

  @Autowired private CustomRefreshTokenGranter customRefreshTokenGranter;

  @Autowired private SpannerAuthorizationCodeServices spannerAuthorizationCodeServices;

  @Bean
  public DefaultOAuth2RequestFactory defaultOAuth2RequestFactory() {
    return new DefaultOAuth2RequestFactory(identityClientDetailsService);
  }

  @Bean
  public DefaultTokenServices defaultTokenServices() {
    DefaultTokenServices tokenServices = new DefaultTokenServices();
    tokenServices.setTokenStore(tokenStore);
    tokenServices.setSupportRefreshToken(true);
    tokenServices.setReuseRefreshToken(false);
    tokenServices.setClientDetailsService(identityClientDetailsService);
    tokenServices.setTokenEnhancer(jwtAccessTokenConverter);
    return tokenServices;
  }

  @Override
  public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
    oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("permitAll()");
  }

  @Override
  public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
    clients.withClientDetails(identityClientDetailsService);
  }

  private List<TokenGranter> getDefaultTokenGranters() {
    AuthorizationServerTokenServices tokenServices = defaultTokenServices();
    OAuth2RequestFactory requestFactory = defaultOAuth2RequestFactory();

    List<TokenGranter> tokenGranters = new ArrayList<>();
    tokenGranters.add(
        new AuthorizationCodeTokenGranter(
            tokenServices,
            spannerAuthorizationCodeServices,
            identityClientDetailsService,
            requestFactory));
    tokenGranters.add(customRefreshTokenGranter);
    tokenGranters.add(
        new ClientCredentialsTokenGranter(
            tokenServices, identityClientDetailsService, requestFactory));
    return tokenGranters;
  }

  private TokenGranter tokenGranter() {
    return new TokenGranter() {
      private CompositeTokenGranter delegate;

      @Override
      public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        if (delegate == null) {
          delegate = new CompositeTokenGranter(getDefaultTokenGranters());
        }
        OAuth2AccessToken token = delegate.grant(grantType, tokenRequest);
        // clear client secret which stored at password match
        RequestContextHolder.currentRequestAttributes()
            .removeAttribute("clientSecret", SCOPE_REQUEST);
        return token;
      }
    };
  }

  @Override
  public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
    // don't use defaultTokenServices bean to config here
    // because the user service is different
    endpoints
        .tokenStore(tokenStore)
        .reuseRefreshTokens(false)
        .authenticationManager(authenticationManager)
        .accessTokenConverter(jwtAccessTokenConverter)
        .userDetailsService(identityUserDetailsService)
        .tokenGranter(tokenGranter())
        .authorizationCodeServices(spannerAuthorizationCodeServices)
        .pathMapping("/oauth/token", "/oauth/" + ApiVersion.CURRENT + "/token")
        .pathMapping("/oauth/authorize", "/oauth/" + ApiVersion.CURRENT + "/authorize")
        .pathMapping("/oauth/confirm_access", "/oauth/" + ApiVersion.CURRENT + "/confirm_access")
        .pathMapping("/oauth/error", "/oauth/" + ApiVersion.CURRENT + "/error")
        .pathMapping("/oauth/check_token", "/oauth/" + ApiVersion.CURRENT + "/check_token")
        .pathMapping("/oauth/token_key", "/oauth/" + ApiVersion.CURRENT + "/token_key");
  }
}

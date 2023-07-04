package com.stubhub.identity.token.service.config;

import com.stubhub.identity.token.service.dye.DyeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
class OAuth2ClientConfig {

  @Value("${security.oauth2.client.clientId}")
  private String clientId;

  @Value("${security.oauth2.client.clientSecret}")
  private String clientSecret;

  @Value("${security.oauth2.client.accessTokenUri}")
  private String accessTokenUrl;

  @Bean
  protected ClientCredentialsResourceDetails appTokenResource() {
    ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
    resource.setAccessTokenUri(accessTokenUrl);
    resource.setClientId(clientId);
    resource.setClientSecret(clientSecret);
    return resource;
  }

  @Bean
  public OAuth2RestTemplate oauth2RestTemplate() {
    OAuth2RestTemplate oAuth2RestTemplate =
        new OAuth2RestTemplate(
            appTokenResource(), new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest()));

    oAuth2RestTemplate.getInterceptors().add(new DyeInterceptor());
    return oAuth2RestTemplate;
  }
}

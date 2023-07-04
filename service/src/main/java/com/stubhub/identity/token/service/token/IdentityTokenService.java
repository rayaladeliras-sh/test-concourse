package com.stubhub.identity.token.service.token;

import com.stubhub.identity.token.service.client.IdentityClientDetailsService;
import com.stubhub.identity.token.service.user.IdentityUserDetails;
import com.stubhub.identity.token.service.user.IdentityUserDetailsService;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenGranter;
import org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Lazy
@Slf4j
@Service
public class IdentityTokenService {

  @Autowired private IdentityUserDetailsService identityUserDetailsService;
  @Autowired private IdentityClientDetailsService identityClientDetailsService;
  @Autowired private DefaultTokenServices defaultTokenServices;
  @Autowired private DefaultOAuth2RequestFactory defaultOAuth2RequestFactory;

  private final OAuth2RequestValidator oAuth2RequestValidator = new DefaultOAuth2RequestValidator();
  private TokenGranter passwordTokenGranter = null;
  private TokenGranter clientTokenGranter = null;

  @PostConstruct
  public void init() {

    if (null == passwordTokenGranter) {
      AuthenticationManager authenticationManager =
          authentication -> {
            UserDetails userDetails =
                identityUserDetailsService.loadUserByUsername(
                    authentication.getPrincipal().toString());
            if (authentication.getCredentials().equals(userDetails.getPassword())) {
              UsernamePasswordAuthenticationToken result =
                  new UsernamePasswordAuthenticationToken(
                      authentication.getPrincipal(),
                      authentication.getCredentials(),
                      userDetails.getAuthorities());
              return result;
            }
            return null;
          };
      passwordTokenGranter =
          new ResourceOwnerPasswordTokenGranter(
              authenticationManager,
              defaultTokenServices,
              identityClientDetailsService,
              defaultOAuth2RequestFactory);
    }

    if (null == clientTokenGranter) {
      clientTokenGranter =
          new ClientCredentialsTokenGranter(
              defaultTokenServices, identityClientDetailsService, defaultOAuth2RequestFactory);
    }
  }

  public OAuth2AccessToken generateUserTokenByUserId(InnerIssueTokenDto innerIssueTokenDto) {
    return generateUserTokenByUserId(innerIssueTokenDto, Collections.EMPTY_MAP);
  }

  public OAuth2AccessToken generateUserTokenByUserId(
      InnerIssueTokenDto innerIssueTokenDto, Map<String, String> requestParams) {

    log.info(
        "method=generateUserTokenByUserId, message=\"The parameter for generate user token is innerIssueTokenDto={}\"",
        innerIssueTokenDto);

    if (StringUtils.isEmpty(innerIssueTokenDto.getClientId())) {
      throw new IllegalArgumentException("Client id is required in request body");
    }
    if (StringUtils.isEmpty(innerIssueTokenDto.getGuid())) {
      throw new IllegalArgumentException("User id is required in request body");
    }

    IdentityUserDetails identityUserDetails;
    if (!StringUtils.isEmpty(innerIssueTokenDto.getEmail())) {
      identityUserDetails =
          IdentityUserDetails.builder()
              .email(innerIssueTokenDto.getEmail())
              .guid(innerIssueTokenDto.getGuid())
              .status("ACTIVE")
              .build();
      identityUserDetailsService.put2Cache(identityUserDetails);
    } else {
      identityUserDetails = identityUserDetailsService.findUserByGuid(innerIssueTokenDto.getGuid());
    }

    return requestUserToken(innerIssueTokenDto, identityUserDetails, requestParams);
  }

  public OAuth2AccessToken requestUserToken(
      InnerIssueTokenDto innerIssueTokenDto,
      IdentityUserDetails identityUserDetails,
      Map<String, String> requestParams) {

    HashMap<String, String> parameters = new LinkedHashMap<>();
    parameters.put("grant_type", "password");
    parameters.put("username", identityUserDetails.getEmail());
    parameters.put("password", identityUserDetailsService.getRandomPassword());
    parameters.putAll(requestParams);

    ClientDetails authenticatedClient =
        identityClientDetailsService.loadClientByClientId(innerIssueTokenDto.getClientId());
    TokenRequest tokenRequest =
        defaultOAuth2RequestFactory.createTokenRequest(parameters, authenticatedClient);
    if (authenticatedClient != null) {
      oAuth2RequestValidator.validateScope(tokenRequest, authenticatedClient);
    }
    return passwordTokenGranter.grant(tokenRequest.getGrantType(), tokenRequest);
  }

  public OAuth2AccessToken requestAppToken(String clientId, String clientSecret) {

    HashMap<String, String> parameters = new LinkedHashMap<>();
    parameters.put("grant_type", "client_credentials");
    parameters.put("clientId", clientId);
    parameters.put("clientSecret", clientSecret);

    ClientDetails authenticatedClient = identityClientDetailsService.loadClientByClientId(clientId);
    TokenRequest tokenRequest =
        defaultOAuth2RequestFactory.createTokenRequest(parameters, authenticatedClient);
    if (authenticatedClient != null) {
      oAuth2RequestValidator.validateScope(tokenRequest, authenticatedClient);
    }
    return clientTokenGranter.grant(tokenRequest.getGrantType(), tokenRequest);
  }
}

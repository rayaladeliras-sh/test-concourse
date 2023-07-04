package com.stubhub.identity.token.service.token.converter;

import static com.stubhub.identity.token.service.token.OAuthConstants.GRANT_TYPE_AUTHORIZATION_CODE;
import static com.stubhub.identity.token.service.token.OAuthConstants.ID_TOKEN;
import static com.stubhub.identity.token.service.token.OAuthConstants.SCOPE_OPENID;

import com.stubhub.identity.token.service.oidc.IDTokenService;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
public class CustomJwtAccessTokenConverter extends JwtAccessTokenConverter {

  @Autowired private IDTokenService idTokenService;
  @Autowired private KeyPair keyPair;
  private final JsonParser objectMapper = JsonParserFactory.create();
  private RsaSigner signer = null;

  @PostConstruct
  public void init() {
    PrivateKey privateKey = keyPair.getPrivate();
    Assert.state(privateKey instanceof RSAPrivateKey, "KeyPair must be an RSA ");
    signer = new RsaSigner((RSAPrivateKey) privateKey);
  }

  @Override
  public OAuth2AccessToken enhance(
      OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
    DefaultOAuth2AccessToken result = new DefaultOAuth2AccessToken(accessToken);
    Map<String, Object> info = new LinkedHashMap<>(accessToken.getAdditionalInformation());
    String tokenId = result.getValue();
    if (!info.containsKey(TOKEN_ID)) {
      info.put(TOKEN_ID, tokenId);
    }
    result.setAdditionalInformation(info);
    String token = encode(result, authentication);
    result.setValue(token);

    if (isOIDCSupport(authentication.getOAuth2Request())) {
      info.put(ID_TOKEN, idTokenService.generateIdToken(token, authentication));
      result.setAdditionalInformation(info);
    }

    return result;
  }

  private boolean isOIDCSupport(OAuth2Request oAuth2Request) {
    Predicate<OAuth2Request> oidcRequestPredicate =
        request ->
            GRANT_TYPE_AUTHORIZATION_CODE.equalsIgnoreCase(request.getGrantType())
                && null != request.getScope()
                && request.getScope().contains(SCOPE_OPENID);
    return oidcRequestPredicate.test(oAuth2Request);
  }

  protected String encode(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
    String content;
    try {
      content =
          objectMapper.formatMap(
              getAccessTokenConverter().convertAccessToken(accessToken, authentication));
    }
    // avoid convert client error to server error
    catch (RestClientResponseException exception) {
      log.error(
          "method=encode Cannot convert access token error: {}",
          exception.getResponseBodyAsString());
      throw OAuth2Exception.create(OAuth2Exception.INVALID_TOKEN, "USER_NOT_FOUND");
    } catch (Exception e) {
      log.error(
          "method=encode Cannot convert access token to JSON error: {}", e.getLocalizedMessage());
      throw new IllegalStateException("Cannot convert access token to JSON", e);
    }
    String token = JwtHelper.encode(content, signer).getEncoded();
    return token;
  }
}

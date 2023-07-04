package com.stubhub.identity.token.service.oidc;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.stubhub.identity.token.service.config.KeyStoreConfig;
import com.stubhub.identity.token.service.token.OAuthConstants;
import com.stubhub.identity.token.service.user.IdentityUserDetails;
import com.stubhub.identity.token.service.user.IdentityUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.Signer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class IDTokenService {

  @Autowired private KeyPair keyPair;
  @Autowired private KeyStore keyStore;
  @Autowired private IdentityUserDetailsService identityUserDetailsService;
  private Signer signer;

  @PostConstruct
  public void init() {
    PrivateKey privateKey = keyPair.getPrivate();
    Assert.state(privateKey instanceof RSAPrivateKey, "KeyPair must be an RSA ");
    signer = new RsaSigner((RSAPrivateKey) privateKey);
  }

  public String generateIdToken(String token, OAuth2Authentication authentication) {
    Map<String, Object> oidcClaims = buildOidcClaims(token, authentication);
    return JwtHelper.encode(JSON.toJSONString(oidcClaims), signer, buildHeader()).getEncoded();
  }

  public Map<String, String> buildHeader() {
    HashMap<String, String> headers = new HashMap<>();
    try {
      MessageDigest digestValue = MessageDigest.getInstance("SHA-1");
      byte[] der = keyStore.getCertificate(KeyStoreConfig.CLOUD_KEY_ALIAS).getEncoded();
      digestValue.update(der);
      byte[] digestInBytes = digestValue.digest();
      String publicCertThumbprint = hexify(digestInBytes);
      String base64UrlEncodedThumbPrint =
          new String(
              new Base64(0, null, true).encode(publicCertThumbprint.getBytes(Charsets.UTF_8)),
              Charsets.UTF_8);
      headers.put("xt5", base64UrlEncodedThumbPrint);
    } catch (NoSuchAlgorithmException | CertificateEncodingException | KeyStoreException e) {
      String error = "Error in generating public cert thumbprint";
      log.error("method=buildHeader {} {}", error, e.getLocalizedMessage());
    }
    return headers;
  }

  private String hexify(byte[] bytes) {
    char[] hexDigits = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    StringBuilder buf = new StringBuilder(bytes.length * 2);
    for (byte aByte : bytes) {
      buf.append(hexDigits[((aByte & 0xF0) >> 4)]);
      buf.append(hexDigits[(aByte & 0xF)]);
    }
    return buf.toString();
  }

  private Map<String, Object> buildOidcClaims(String token, OAuth2Authentication authentication) {
    HashMap<String, Object> claims = new HashMap<>();
    claims.putAll(buildUserClaims(authentication));
    claims.putAll(buildStandardClaims(token, authentication));
    return claims;
  }

  public Map<String, Object> buildUserClaims(OAuth2Authentication authentication) {
    String username = authentication.getUserAuthentication().getName();
    IdentityUserDetails userDetails = identityUserDetailsService.findUserByEmail(username);
    // issue user token will make cache directly if passing email
    // so we need to update the cache
    if (null == userDetails.getName()) {
      identityUserDetailsService.removeUserFromCache(userDetails.getGuid());
      userDetails = identityUserDetailsService.findUserByEmail(username);
    }
    Map<String, Object> userClaims = new HashMap<>();

    userClaims.put(OAuthConstants.CLAIM_SUB, userDetails.getGuid());

    Set<String> scopes = authentication.getOAuth2Request().getScope();
    for (String scope : scopes) {
      userClaims.putAll(ScopedClaim.buildClaims(scope, userDetails));
    }

    return userClaims;
  }

  private Map<String, Object> buildStandardClaims(
      String token, OAuth2Authentication authentication) {

    JwtClaims claims = new JwtClaims();
    claims.setJwtId(UUID.randomUUID().toString());
    claims.setIssuer(OAuthConstants.DEFAULT_CLAIM_ISSUE_URL_VALUE);
    claims.setStringListClaim(
        OAuthConstants.CLAIM_AUDIENCE,
        Arrays.asList(authentication.getOAuth2Request().getClientId()));
    NumericDate expirationDate = NumericDate.now();
    expirationDate.addSeconds(3600);
    claims.setExpirationTime(expirationDate);
    claims.setIssuedAtToNow();
    try {
      claims.setClaim(OAuthConstants.CLAIM_AUTH_TIME, claims.getIssuedAt().getValue());
    } catch (MalformedClaimException e) {
      log.error("method=buildStandardClaims get issue at error: {}", e.getLocalizedMessage());
      claims.setClaim(OAuthConstants.CLAIM_AUTH_TIME, System.currentTimeMillis() / 1000);
    }
    claims.setStringClaim(
        OAuthConstants.CLAIM_AZP, authentication.getOAuth2Request().getClientId());

    try {
      claims.setStringClaim(OAuthConstants.CLAIM_AT_HASH, getHashValue(token));
      Map<String, String> requestParameters =
          authentication.getOAuth2Request().getRequestParameters();
      if (null != requestParameters) {
        String nonce = requestParameters.get(OAuthConstants.CLAIM_NONCE);
        if (!StringUtils.isEmpty(nonce)) {
          claims.setStringClaim(OAuthConstants.CLAIM_NONCE, nonce);
        }
      }
    } catch (Exception e) {
      log.error("method=buildStandardClaims generate at_hash fail:{}", e.getLocalizedMessage());
    }
    return claims.getClaimsMap();
  }

  public String getHashValue(String value) throws NoSuchAlgorithmException {
    String digAlg = "SHA-256";
    MessageDigest md = MessageDigest.getInstance(digAlg);
    md.update(value.getBytes(StandardCharsets.UTF_8));
    byte[] digest = md.digest();
    int leftHalfBytes = 16;
    byte[] leftmost = new byte[leftHalfBytes];
    System.arraycopy(digest, 0, leftmost, 0, leftHalfBytes);
    return new String(Base64.encodeBase64URLSafe(leftmost), StandardCharsets.UTF_8);
  }
}

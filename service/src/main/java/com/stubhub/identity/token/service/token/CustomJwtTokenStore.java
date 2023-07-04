package com.stubhub.identity.token.service.token;

import com.stubhub.identity.token.service.auditlog.Audit;
import com.stubhub.identity.token.service.auditlog.AuditLogService;
import com.stubhub.identity.token.service.auditlog.dto.RemoveRefreshTokenAuditData;
import com.stubhub.identity.token.service.auditlog.enums.AuditStatusEnum;
import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import com.stubhub.identity.token.service.token.refresh.OAuthRefreshTokenService;
import com.stubhub.identity.token.service.utils.SerializationUtils;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Slf4j
public class CustomJwtTokenStore extends JwtTokenStore {

  private static final int DEFAULT_FLUSH_INTERVAL = 256;

  private final OAuthRefreshTokenService oAuthRefreshTokenService;
  private final AuditLogService auditLogService;

  private final DelayQueue<CustomJwtTokenStore.TokenExpiry> expiryQueue = new DelayQueue<>();

  private final ConcurrentHashMap<String, CustomJwtTokenStore.TokenExpiry> expiryMap =
      new ConcurrentHashMap<>();

  private int flushInterval = DEFAULT_FLUSH_INTERVAL;

  private final AtomicInteger flushCounter = new AtomicInteger(0);

  /**
   * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices
   * if used).
   *
   * @param jwtTokenEnhancer
   */
  public CustomJwtTokenStore(
      JwtAccessTokenConverter jwtTokenEnhancer,
      OAuthRefreshTokenService oAuthRefreshTokenService,
      AuditLogService auditLogService) {
    super(jwtTokenEnhancer);
    this.oAuthRefreshTokenService = oAuthRefreshTokenService;
    this.auditLogService = auditLogService;
  }

  public int getFlushInterval() {
    return flushInterval;
  }

  public void setFlushInterval(int flushInterval) {
    this.flushInterval = flushInterval;
  }

  @Override
  @Audit(method = AuditedMethodEnum.STORE_REFRESH_TOKEN)
  public void storeRefreshToken(
      OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
    // i think it's an issue of spring, client credentials doesn't need refresh token
    // but this method will still be called without call removeRefreshToken afterwards
    if (null != authentication.getOAuth2Request()
        && "client_credentials"
            .equalsIgnoreCase(authentication.getOAuth2Request().getGrantType())) {
      return;
    }

    Date expiration = null;
    if (refreshToken instanceof DefaultExpiringOAuth2RefreshToken) {
      if (this.flushCounter.incrementAndGet() >= this.flushInterval) {
        flush();
        this.flushCounter.set(0);
      }
      expiration = ((DefaultExpiringOAuth2RefreshToken) refreshToken).getExpiration();
      if (expiration != null) {
        CustomJwtTokenStore.TokenExpiry expiry =
            new CustomJwtTokenStore.TokenExpiry(refreshToken.getValue(), expiration);
        // Remove existing expiry for this token if present
        expiryQueue.remove(expiryMap.put(refreshToken.getValue(), expiry));
        this.expiryQueue.put(expiry);
      }
    }

    log.debug(
        "method=storeRefreshToken generate refresh token {} for user {} by client {} at {} expire date is {} ",
        refreshToken.getValue(),
        authentication.getPrincipal(),
        authentication.getOAuth2Request().getClientId(),
        new Date(),
        expiration);
    oAuthRefreshTokenService.save(
        refreshToken.getValue(),
        SerializationUtils.serialize(refreshToken),
        SerializationUtils.serialize(authentication),
        expiration);
  }

  @Override
  public OAuth2RefreshToken readRefreshToken(String tokenValue) {
    return oAuthRefreshTokenService.readRefreshTokenForRefreshToken(tokenValue);
  }

  @Override
  public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
    return oAuthRefreshTokenService.readAuthenticationForRefreshToken(token.getValue());
  }

  @Override
  public void removeRefreshToken(OAuth2RefreshToken token) {
    log.info("method=removeRefreshToken {} was refreshed", token);
    removeRefreshToken(token.getValue());
  }

  public void removeRefreshToken(String tokenId) {
    oAuthRefreshTokenService.deleteById(tokenId);
  }

  private void flush() {
    CustomJwtTokenStore.TokenExpiry expiry = expiryQueue.poll();
    while (expiry != null) {
      log.info(
          "method=flush refresh token was expired and remove from db, tokenId={}",
          expiry.getValue());

      RemoveRefreshTokenAuditData auditData = new RemoveRefreshTokenAuditData();
      auditData.setToken(expiry.getValue());

      try {
        removeRefreshToken(expiry.getValue());

        auditData.setStatus(AuditStatusEnum.SUCCESS.name().toLowerCase());
        auditData.setFailReason("");
        auditLogService.publishAuditLog(AuditedMethodEnum.FLUSH_REFRESH_TOKEN, auditData);
      } catch (RuntimeException e) {
        auditData.setStatus(AuditStatusEnum.FAILURE.name().toLowerCase());
        auditData.setFailReason(e.getMessage());
        auditLogService.publishAuditLog(AuditedMethodEnum.FLUSH_REFRESH_TOKEN, auditData);
      }

      expiry = expiryQueue.poll();
    }
  }

  private static class TokenExpiry implements Delayed {

    private final long expiry;

    private final String value;

    public TokenExpiry(String value, Date date) {
      this.value = value;
      this.expiry = date.getTime();
    }

    public int compareTo(Delayed other) {
      if (this == other) {
        return 0;
      }
      long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
      return (diff == 0 ? 0 : ((diff < 0) ? -1 : 1));
    }

    public long getDelay(TimeUnit unit) {
      return expiry - System.currentTimeMillis();
    }

    public String getValue() {
      return value;
    }
  }
}

package com.stubhub.identity.token.service.token.refresh;

import com.stubhub.identity.token.service.auditlog.Audit;
import com.stubhub.identity.token.service.auditlog.AuditLogService;
import com.stubhub.identity.token.service.auditlog.dto.ClearExpiredRefreshTokenAuditData;
import com.stubhub.identity.token.service.auditlog.enums.AuditStatusEnum;
import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import com.stubhub.identity.token.service.utils.SerializationUtils;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OAuthRefreshTokenService {

  @Autowired private OAuthRefreshTokenRepository refreshTokenRepository;
  @Autowired private AuditLogService auditLogService;

  @Transactional
  public void save(String tokenId, String token, String authentication, Date expired) {
    log.debug("method=save tokenId={}", tokenId);
    refreshTokenRepository.save(
        OAuthRefreshToken.builder()
            .tokenId(tokenId)
            .token(token)
            .authentication(authentication)
            .createAt(new Date())
            .expiredAt(expired)
            .build());
  }

  @Transactional(readOnly = true)
  public String getTokenById(String tokenId) {
    Optional<OAuthRefreshToken> refreshToken = refreshTokenRepository.findById(tokenId);
    if (refreshToken.isPresent()) {
      log.debug("method=getTokenById tokenId={} found it", tokenId);
      return refreshToken.get().getToken();
    }
    log.info("method=getTokenById tokenId={} not found", tokenId);
    return null;
  }

  public OAuth2RefreshToken readRefreshTokenForRefreshToken(String tokenId) {
    String token = getTokenById(tokenId);
    if (StringUtils.isEmpty(token)) {
      return null;
    }
    return SerializationUtils.deserialize(token);
  }

  @Transactional(readOnly = true)
  public String getAuthenticationById(String tokenId) {
    Optional<OAuthRefreshToken> refreshToken = refreshTokenRepository.findById(tokenId);
    if (refreshToken.isPresent()) {
      log.debug("method=getAuthenticationById tokenId={} found it", tokenId);
      return refreshToken.get().getAuthentication();
    }
    log.info("method=getAuthenticationById tokenId={} not found", tokenId);
    return null;
  }

  public OAuth2Authentication readAuthenticationForRefreshToken(String tokenId) {
    String authentication = getAuthenticationById(tokenId);
    if (StringUtils.isEmpty(authentication)) {
      return null;
    }
    return SerializationUtils.deserialize(authentication);
  }

  @Transactional
  @Audit(method = AuditedMethodEnum.DELETE_REFRESH_TOKEN)
  public void deleteById(String tokenId) {
    log.debug("method=deleteById tokenId={}", tokenId);
    refreshTokenRepository.deleteById(tokenId);
  }

  @Transactional(readOnly = true)
  public boolean isExistInDB(String tokenId) {
    boolean exist = refreshTokenRepository.findById(tokenId).isPresent();
    if (!exist) {
      log.info("method=isExistInDB tokenId={} not exist", tokenId);
    } else {
      log.debug("method=isExistInDB tokenId={} exist", tokenId);
    }
    return exist;
  }

  @Scheduled(fixedDelayString = "${refresh-token.task.clear.fixedRate}", initialDelay = 1000)
  public void clearExpiredRefreshToken() {
    Optional<List<OAuthRefreshToken>> wrapperExpiredRefreshTokens =
        refreshTokenRepository.getExpiredRefreshToken();
    if (wrapperExpiredRefreshTokens.isPresent() && wrapperExpiredRefreshTokens.get().size() > 0) {
      List<OAuthRefreshToken> expiredRefreshTokens = wrapperExpiredRefreshTokens.get();
      List<String> tokenIds =
          expiredRefreshTokens
              .stream()
              .map(OAuthRefreshToken::getTokenId)
              .collect(Collectors.toList());

      ClearExpiredRefreshTokenAuditData auditData = new ClearExpiredRefreshTokenAuditData();
      auditData.setTokenIds(String.join(",", tokenIds));


      try {
        log.info(
            "method=clearExpiredRefreshToken clear expired refresh token count={}, list={}",
            tokenIds.size(),
            tokenIds);
        refreshTokenRepository.deleteAll(expiredRefreshTokens);

        auditData.setStatus(AuditStatusEnum.SUCCESS.name().toLowerCase());
        auditData.setFailReason("");
        auditLogService.publishAuditLog(AuditedMethodEnum.CLEAR_EXPIRED_REFRESH_TOKEN, auditData);
      } catch (RuntimeException e) {
        auditData.setStatus(AuditStatusEnum.FAILURE.name().toLowerCase());
        auditData.setFailReason(e.getMessage());
        auditLogService.publishAuditLog(AuditedMethodEnum.CLEAR_EXPIRED_REFRESH_TOKEN, auditData);
      }
    }
  }
}

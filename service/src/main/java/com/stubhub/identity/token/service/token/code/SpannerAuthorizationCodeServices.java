package com.stubhub.identity.token.service.token.code;

import com.google.cloud.spanner.Statement;
import com.stubhub.identity.token.service.utils.SerializationUtils;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.code.RandomValueAuthorizationCodeServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SpannerAuthorizationCodeServices extends RandomValueAuthorizationCodeServices {

  @Autowired private OAuthCodeRepository oAuthCodeRepository;

  @Autowired private SpannerTemplate spannerTemplate;

  @Value("${code.max.inactive.interval}")
  private long maxInactiveInterval;

  @Transactional
  @Override
  protected void store(String code, OAuth2Authentication authentication) {

    OAuthCode oAuthCode =
        OAuthCode.builder()
            .code(code)
            .authentication(SerializationUtils.serialize(authentication))
            .createAt(new Date())
            .build();

    oAuthCodeRepository.save(oAuthCode);
  }

  @Transactional
  public OAuth2Authentication remove(String code) {

    Optional<OAuthCode> oAuthCode = oAuthCodeRepository.findTopByCode(code);

    if (oAuthCode.isPresent() && null != oAuthCode.get().getAuthentication()) {
      oAuthCodeRepository.delete(oAuthCode.get());
      return SerializationUtils.deserialize(oAuthCode.get().getAuthentication());
    } else {
      return null;
    }
  }

  @Scheduled(fixedDelayString = "${code.task.clear.fixedRate}", initialDelay = 60000)
  public void scheduleClearExpiredCode() {
    String sql =
        String.format(
            "delete from OAUTH_CODE where (UNIX_SECONDS(CURRENT_TIMESTAMP) - UNIX_SECONDS(CREATED_AT)) > %d",
            maxInactiveInterval);
    log.info("method=scheduleClearExpiredCode start clear expired code from spanner");
    long deleted = spannerTemplate.executeDmlStatement(Statement.newBuilder(sql).build());
    log.info("method=scheduleClearExpiredCode delete {} rows", deleted);
  }
}

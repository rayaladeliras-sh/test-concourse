package com.stubhub.identity.token.service.session;

import com.stubhub.identity.token.service.token.cloud.CloudTokenService;
import com.stubhub.identity.token.service.token.shape.ShapeTokenService;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShapeSessionService {

  @Autowired private ShapeTokenService shapeTokenService;

  @Autowired private CloudTokenService cloudTokenService;

  @Lazy @Autowired private DefaultCookieSerializer myCookieSerializer;

  @Autowired private CacheManager cacheManager;

  @Value("${server.servlet.session.cookie.legacy}")
  private String legacyCookieName;

  @Cacheable("legacySession")
  public Authentication validateSession(String sessionId) {
    return cloudTokenService.validateSession(sessionId);
  }

  public void invalidateSession(HttpServletRequest request) {
    List<String> cookies = myCookieSerializer.readCookieValues(request);
    if (null != cookies && !cookies.isEmpty()) {
      Optional<String> cookieItem =
          cookies.stream().filter(cookie -> cookie.contains(legacyCookieName)).findFirst();
      if (cookieItem.isPresent()) {
        removeFromCache(cookieItem.get());
        shapeTokenService.logout(cookieItem.get());
        log.info("method=invalidateSession invalid session from cookie:{}", cookieItem);
      }
    }
  }

  private void removeFromCache(String cookie) {
    if (StringUtils.isBlank(cookie) || !cookie.contains("=")) {
      log.warn("method=removeFromCache invalid legacy session cookie item:{}", cookie);
    } else {
      String[] sessionItems = cookie.split("=");
      if (2 == sessionItems.length) {
        cacheManager.getCache("legacySession").evict(sessionItems[1]);
      } else {
        log.warn("method=removeFromCache invalid legacy session cookie item:{}", cookie);
      }
    }
  }
}

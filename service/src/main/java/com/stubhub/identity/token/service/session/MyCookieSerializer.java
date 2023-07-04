package com.stubhub.identity.token.service.session;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.util.StringUtils;

@Data
@Slf4j
public class MyCookieSerializer extends DefaultCookieSerializer {

  private String legacyCookieName;
  private ShapeSessionService shapeSessionService;

  public MyCookieSerializer(ShapeSessionService shapeSessionService, String legacyCookieName) {
    this.shapeSessionService = shapeSessionService;
    this.legacyCookieName = legacyCookieName;
  }

  public List<String> readCookieValues(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    log.info(
        "method=readCookieValues request url={} cookie={}",
        request.getRequestURL().toString(),
        cookies);
    List<String> matchingCookieValues = new ArrayList<>();
    String sessionId = null;
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (this.legacyCookieName.equals(cookie.getName())) {
          sessionId = cookie.getValue();
          if (sessionId == null) {
            continue;
          }
          Authentication authentication = shapeSessionService.validateSession(sessionId);
          log.info(
              "method=readCookieValues legacy session:{} is valid={}",
              sessionId,
              null != authentication);
          if (null == authentication) {
            log.warn(
                "method=readCookieValues get invalid legacy session:{} from cookie", sessionId);
            continue;
          }
          matchingCookieValues.add(cookie.getName() + "=" + sessionId);
        }
      }
    }

    if (matchingCookieValues.size() > 0) {
      return matchingCookieValues;
    } else {
      List<String> parentCookieValues = super.readCookieValues(request);
      if (null != parentCookieValues && !StringUtils.isEmpty(sessionId)) {
        parentCookieValues.remove(sessionId);
      }
      if (null != parentCookieValues) {
        parentCookieValues.forEach(
            session ->
                log.info("method=readCookieValues get token mgt session:{} from cookie", session));
      }
      return parentCookieValues;
    }
  }
}

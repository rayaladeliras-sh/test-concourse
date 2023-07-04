package com.stubhub.identity.token.service.session;

import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CustomizedSessionStore implements SessionRepository<MapSession> {

  @Autowired private CloudSessionService cloudSessionService;

  @Value("${session.max.inactive.interval}")
  private long maxInactiveInterval;

  @Value("${server.servlet.session.cookie.legacy}")
  private String legacyCookieName = "SH_SI";

  @Autowired private ShapeSessionService shapeSessionService;

  @Override
  public MapSession createSession() {
    return createSession(UUID.randomUUID().toString());
  }

  private MapSession createSession(String id) {
    MapSession result = new MapSession(id);
    result.setMaxInactiveInterval(Duration.ofSeconds(maxInactiveInterval));
    log.debug("create new session, session id: {}", result.getId());
    return result;
  }

  @Override
  public void save(MapSession mapSession) {
    if (!mapSession.getId().equals(mapSession.getOriginalId())) {
      cloudSessionService.remove(mapSession.getOriginalId());
    }
    log.debug("save session to database, session id: {}", mapSession.getId());
    cloudSessionService.put(mapSession);
  }

  private MapSession findFromLocal(String s) {
    Session saved = cloudSessionService.get(s);
    if (saved == null) {
      return null;
    } else if (saved.isExpired()) {
      this.deleteById(saved.getId());
      return null;
    } else {
      return new MapSession(saved);
    }
  }

  @Override
  public MapSession findById(String s) {
    // check site with first priority
    if (!StringUtils.isEmpty(s) && s.startsWith(legacyCookieName)) {
      String realId = s.substring(legacyCookieName.length() + 1);
      MapSession mapSession = findFromLocal(realId);
      if (null == mapSession) {
        log.info("can not find session from cloud database");
        Authentication authentication = shapeSessionService.validateSession(realId);
        if (null != authentication) {
          mapSession = createSession(realId);
          SecurityContextHolder.getContext().setAuthentication(authentication);
          mapSession.setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
              SecurityContextHolder.getContext());
          save(mapSession);
          log.info("get session from shape and save into cloud database");
          return mapSession;
        }
      }
      return mapSession;
    } else {
      return findFromLocal(s);
    }
  }

  @Override
  public void deleteById(String s) {
    cloudSessionService.remove(s);
  }
}

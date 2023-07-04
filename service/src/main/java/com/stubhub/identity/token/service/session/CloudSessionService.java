package com.stubhub.identity.token.service.session;

import com.google.cloud.spanner.Statement;
import com.stubhub.identity.token.service.utils.SerializationUtils;
import java.sql.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.MapSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CloudSessionService {
  @Autowired private SpannerSessionRepository spannerSessionRepository;

  @Value("${session.max.inactive.interval}")
  private long maxInactiveInterval;

  @Autowired private SpannerTemplate spannerTemplate;

  @Transactional
  public void put(MapSession session) {
    SessionEntity entity =
        SessionEntity.builder()
            .sessionId(session.getId())
            .session(SerializationUtils.serialize(session))
            .createAt(Date.from(session.getCreationTime()))
            .lastAccess(Date.from(session.getLastAccessedTime()))
            .build();

    log.debug("method=put save session into database {}", session.getId());
    spannerSessionRepository.save(entity);
  }

  public MapSession get(String id) {
    Optional<SessionEntity> entity = spannerSessionRepository.findById(id);
    if (entity.isPresent() && null != entity.get().getSession()) {
      log.debug("method=get get session from database {}", id);
      return SerializationUtils.deserialize(entity.get().getSession());
    } else {
      log.debug("method=get can't get session from database {}", id);
      return null;
    }
  }

  public void remove(String id) {
    log.debug("method=remove delete session from database {}", id);
    spannerSessionRepository.deleteById(id);
  }

  @Scheduled(fixedDelayString = "${session.task.clear.fixedRate}", initialDelay = 1000)
  public void scheduleClearExpiredSession() {
    String sql =
        String.format(
            "delete from session where (UNIX_SECONDS(CURRENT_TIMESTAMP) - UNIX_SECONDS(session.LAST_ACCESS)) > %d",
            maxInactiveInterval);
    log.info("method=scheduleClearExpiredSession start clear expired session from spanner");
    long deleted = spannerTemplate.executeDmlStatement(Statement.newBuilder(sql).build());
    log.info("method=scheduleClearExpiredSession delete {} rows", deleted);
  }
}

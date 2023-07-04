package com.stubhub.identity.token.service.auditlog;

import brave.Span;
import brave.Tracer;
import com.stubhub.identity.token.service.auditlog.dto.BaseAuditData;
import com.stubhub.identity.token.service.auditlog.dto.PublishAuditLogRequest;
import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import com.stubhub.identity.token.service.auditlog.remoteapi.AuditV1API;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogService {

  private final static String AUDIT_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";

  private final Tracer tracer;
  private final AuditV1API auditV1API;

  private final ExecutorService executorService = Executors.newFixedThreadPool(5);

  public AuditLogService(Tracer tracer, AuditV1API auditV1API) {
    this.tracer = tracer;
    this.auditV1API = auditV1API;
  }

  public <T extends BaseAuditData> void publishAuditLog(AuditedMethodEnum auditType, T auditData) {
    log.info("class=AuditLogService method=publishAuditLog auditType={} message=\"Publishing the audit log\"",
             auditType.toString());
    executorService.submit(
        createTask(
            buildAuditLogRequest(auditType, auditData)));
  }

  private <T extends BaseAuditData> PublishAuditLogRequest buildAuditLogRequest(
      AuditedMethodEnum auditType, T auditData) {

    Span span = tracer.currentSpan() != null ? tracer.currentSpan() : tracer.nextSpan();

    PublishAuditLogRequest publishAuditLogRequest = new PublishAuditLogRequest();
    publishAuditLogRequest.setAuditType(auditType.toString());
    publishAuditLogRequest.setAuditTime(DateTime.now(DateTimeZone.UTC).toString(AUDIT_TIME_FORMAT));
    publishAuditLogRequest.setAuditData(auditData);
    publishAuditLogRequest.setTraceId(span.context().traceIdString());

    return publishAuditLogRequest;
  }

  private Callable<ResponseEntity<String>> createTask(PublishAuditLogRequest request) {
    return new AuditV1APICallable(auditV1API, request);
  }
}

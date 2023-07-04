package com.stubhub.identity.token.service.auditlog;

import com.stubhub.identity.token.service.auditlog.dto.PublishAuditLogRequest;
import com.stubhub.identity.token.service.auditlog.remoteapi.AuditV1API;
import java.util.concurrent.Callable;
import org.springframework.http.ResponseEntity;

public class AuditV1APICallable implements Callable<ResponseEntity<String>> {

  private final AuditV1API auditV1API;
  private final PublishAuditLogRequest publishAuditLogRequest;

  public AuditV1APICallable(AuditV1API auditV1API, PublishAuditLogRequest publishAuditLogRequest) {
    this.auditV1API = auditV1API;
    this.publishAuditLogRequest = publishAuditLogRequest;
  }

  @Override
  public ResponseEntity<String> call() {
    return auditV1API.publishAuditLog(publishAuditLogRequest);
  }
}

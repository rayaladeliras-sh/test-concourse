package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;

@Data
public class PublishAuditLogRequest {
  private String auditType;
  private String auditTime;
  private String source = "token-mgt";
  private BaseAuditData auditData;
  private String traceId;
}

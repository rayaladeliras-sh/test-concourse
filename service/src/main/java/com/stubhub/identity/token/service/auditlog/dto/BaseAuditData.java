package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;

@Data
public abstract class BaseAuditData {
  private String status;
  private String failReason;
}

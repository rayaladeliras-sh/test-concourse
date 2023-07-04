package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActUserTokenAuditData extends BaseAuditData {
  private String sub;
  private String clientId;
  private String clientName;
  private String roles;
}

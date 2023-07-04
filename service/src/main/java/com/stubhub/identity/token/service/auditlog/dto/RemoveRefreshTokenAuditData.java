package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RemoveRefreshTokenAuditData extends BaseAuditData {
  private String token;
}

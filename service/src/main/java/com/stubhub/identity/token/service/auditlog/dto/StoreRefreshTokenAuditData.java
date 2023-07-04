package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoreRefreshTokenAuditData extends BaseAuditData {
  private String refreshToken;
  private String principal;
  private String clientId;
}

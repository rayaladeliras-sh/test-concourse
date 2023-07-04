package com.stubhub.identity.token.service.auditlog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClearExpiredRefreshTokenAuditData extends BaseAuditData {

  private String tokenIds;
}

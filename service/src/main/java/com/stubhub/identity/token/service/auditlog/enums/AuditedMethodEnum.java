package com.stubhub.identity.token.service.auditlog.enums;

public enum AuditedMethodEnum {
  ACT_USER_TOKEN,
  DELETE_REFRESH_TOKEN,
  CLEAR_EXPIRED_REFRESH_TOKEN,
  STORE_REFRESH_TOKEN,
  FLUSH_REFRESH_TOKEN,
  ;

  AuditedMethodEnum() {
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}

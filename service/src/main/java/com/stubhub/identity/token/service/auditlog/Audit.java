package com.stubhub.identity.token.service.auditlog;

import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
  AuditedMethodEnum method();
}

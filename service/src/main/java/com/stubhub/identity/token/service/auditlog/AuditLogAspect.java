package com.stubhub.identity.token.service.auditlog;

import com.stubhub.identity.token.service.auditlog.dto.ActUserTokenAuditData;
import com.stubhub.identity.token.service.auditlog.dto.RemoveRefreshTokenAuditData;
import com.stubhub.identity.token.service.auditlog.dto.StoreRefreshTokenAuditData;
import com.stubhub.identity.token.service.auditlog.enums.AuditStatusEnum;
import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import com.stubhub.identity.token.service.token.act.ActClaimDto;
import com.stubhub.identity.token.service.token.act.ActUserTokenDto;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuditLogAspect {

  private final AuditLogService auditLogService;

  public AuditLogAspect(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @AfterReturning("@annotation(Audit)")
  public void auditSuccessfulOperations(JoinPoint joinPoint) {
    log.info(
        "class=AuditLogAspect method=auditSuccessfulOperations message=\"Sending the audit log of a successful operation\"");
    publishAuditLog(joinPoint, AuditStatusEnum.SUCCESS, null);
  }

  @AfterThrowing(value = "@annotation(Audit)", throwing = "throwable")
  public void auditFailedOperations(JoinPoint joinPoint, Throwable throwable) {
    log.info(
        "class=AuditLogAspect method=auditFailedOperations message=\"Sending the audit log of a failed operation\"");
    publishAuditLog(joinPoint, AuditStatusEnum.FAILURE, throwable);
  }

  private void publishAuditLog(JoinPoint joinPoint, AuditStatusEnum auditStatusEnum, Throwable throwable) {
    final Signature joinPointSignature = joinPoint.getSignature();
    final String methodName = joinPointSignature.getName();
    final Class<?> methodClass = joinPointSignature.getDeclaringType();
    final Optional<Method> methodOptional =
        Arrays.stream(methodClass.getDeclaredMethods())
            .filter(i -> methodName.equalsIgnoreCase(i.getName()))
            .findFirst();

    if (!methodOptional.isPresent()) {
      log.warn("methodClass={} method={} message=\"No method with this name found in this class\"",
               methodClass.getCanonicalName(),
               methodName);

      return;
    }

    final Method method = methodOptional.get();

    final Audit auditAnnotation = method.getAnnotation(Audit.class);

    final String errorMessage = getErrorMessage(throwable);

    switch (auditAnnotation.method()) {
      case ACT_USER_TOKEN:
        publishActUserTokenAuditLog(joinPoint, auditStatusEnum, errorMessage);
        break;
      case DELETE_REFRESH_TOKEN:
        publishDeleteRefreshTokenAuditLog(joinPoint, auditStatusEnum, errorMessage);
        break;
      case STORE_REFRESH_TOKEN:
        publishStoreRefreshTokenAuditLog(joinPoint, auditStatusEnum, errorMessage);
        break;
      default:
        log.warn("class=AuditLogAspect "
                     + "method=auditFailedOperations "
                     + "annotatedMethod={} "
                     + "message=\"Unknown annotated method\"",
                 auditAnnotation.method());
        break;
    }
  }

  private void publishActUserTokenAuditLog(JoinPoint joinPoint, AuditStatusEnum auditStatusEnum, String errorMessage) {
    ActUserTokenDto actUserTokenDto = (ActUserTokenDto) joinPoint.getArgs()[1];
    ActClaimDto actClaimDto = actUserTokenDto.getAct();

    ActUserTokenAuditData auditData = new ActUserTokenAuditData();
    auditData.setClientId(actClaimDto.getClientId());
    auditData.setClientName(actClaimDto.getClientName());
    if (actClaimDto.getRoles() != null) {
      auditData.setRoles(String.join(",", actClaimDto.getRoles()));
    } else {
      auditData.setRoles("");
    }
    auditData.setSub(actClaimDto.getSub());
    auditData.setFailReason(errorMessage);
    auditData.setStatus(auditStatusEnum.name().toLowerCase());

    auditLogService.publishAuditLog(AuditedMethodEnum.ACT_USER_TOKEN, auditData);
  }

  private void publishDeleteRefreshTokenAuditLog(JoinPoint joinPoint, AuditStatusEnum auditStatusEnum,
      String errorMessage) {
    String token = (String) joinPoint.getArgs()[0];

    RemoveRefreshTokenAuditData auditData = new RemoveRefreshTokenAuditData();
    auditData.setToken(token);
    auditData.setStatus(auditStatusEnum.name().toLowerCase());
    auditData.setFailReason(errorMessage);

    auditLogService.publishAuditLog(AuditedMethodEnum.DELETE_REFRESH_TOKEN, auditData);
  }

  private void publishStoreRefreshTokenAuditLog(JoinPoint joinPoint, AuditStatusEnum auditStatusEnum,
      String errorMessage) {
    OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) joinPoint.getArgs()[0];
    OAuth2Authentication authentication = (OAuth2Authentication) joinPoint.getArgs()[1];

    StoreRefreshTokenAuditData auditData = new StoreRefreshTokenAuditData();
    auditData.setRefreshToken(refreshToken.getValue());
    auditData.setPrincipal(String.valueOf(authentication.getPrincipal()));
    auditData.setClientId(authentication.getOAuth2Request().getClientId());
    auditData.setStatus(auditStatusEnum.name().toLowerCase());
    auditData.setFailReason(errorMessage);

    auditLogService.publishAuditLog(AuditedMethodEnum.STORE_REFRESH_TOKEN, auditData);
  }

  private String getErrorMessage(Throwable throwable) {
    return throwable != null ? throwable.getMessage() : "";
  }
}

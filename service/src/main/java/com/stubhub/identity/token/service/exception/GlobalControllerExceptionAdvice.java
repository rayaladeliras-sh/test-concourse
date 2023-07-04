package com.stubhub.identity.token.service.exception;

import com.google.api.gax.rpc.AlreadyExistsException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice(
    basePackages = {"com.stubhub.identity.token.service.controller", "com.stubhub.token.service"})
@Slf4j
public class GlobalControllerExceptionAdvice {

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    InvalidRequestException.class,
    AlreadyExistsException.class,
    IllegalArgumentException.class,
    ClientRegistrationException.class,
    NoSuchClientException.class
  })
  @ResponseBody
  private ResponseEntity handleBadRequestException(Exception e) {
    return makeResponse(e, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({InsufficientAuthenticationException.class, AccessDeniedException.class})
  @ResponseBody
  private ResponseEntity handleForbiddenException(Exception e) {
    return makeResponse(e, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(AuthenticationException.class)
  @ResponseBody
  private ResponseEntity handleAuthenticationException(AuthenticationException e) {
    return makeResponse(e, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(ExtendedErrorException.class)
  @ResponseBody
  private ResponseEntity handleExtendedErrorException(ExtendedErrorException e) {
    CommonErrorResp error =
        CommonErrorResp.builder()
            .message(e.getReason())
            .path(e.getData().get("path").toString())
            .status(e.getStatus().value())
            .timestamp(Instant.now())
            .error(e.getCause().getMessage())
            .build();
    log.error(
        "statusCode= {}, errMsg= {}, stack={}",
        e.getStatus().value(),
        e.getLocalizedMessage(),
        e.getStackTrace());
    return new ResponseEntity<>(error, e.getStatus());
  }

  @ExceptionHandler(Exception.class)
  @ResponseBody
  private ResponseEntity handleOtherException(Exception e) {
    return makeResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity makeResponse(Exception e, HttpStatus status) {
    CommonErrorResp error =
        CommonErrorResp.builder()
            .message(e.fillInStackTrace().toString())
            .status(status.value())
            .timestamp(Instant.now())
            .error(e.getClass().toString())
            .build();
    log.error(
        "statusCode= {}, errMsg= {}, stack={}",
        status.value(),
        e.getLocalizedMessage(),
        e.getStackTrace());
    return new ResponseEntity<>(error, status);
  }
}

package com.stubhub.identity.token.service.auditlog.remoteapi;

import com.stubhub.identity.token.service.auditlog.dto.PublishAuditLogRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@Component
public class AuditV1API {

  @Value("${remote.api.audit.endpoint}")
  private String endpoint;

  private final OAuth2RestOperations oauth2RestTemplate;

  public AuditV1API(OAuth2RestOperations oauth2RestTemplate) {
    this.oauth2RestTemplate = oauth2RestTemplate;
  }

  public ResponseEntity<String> publishAuditLog(PublishAuditLogRequest request) {
    StopWatch stopWatch = new StopWatch();

    log.info("class=AuditV1API method=publishAuditLog auditType={} message=\"Sending the audit log\"",
             request.getAuditType());

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    HttpEntity<PublishAuditLogRequest> httpEntity = new HttpEntity<>(request, headers);

    try {
      stopWatch.start();
      ResponseEntity<String> responseEntity = oauth2RestTemplate.postForEntity(endpoint, httpEntity, String.class);
      stopWatch.stop();

      log.info("class=AuditV1API method=publishAuditLog auditType={} duration={}ms message=\"Audit log sent\"",
               request.getAuditType(),
               stopWatch.getLastTaskTimeMillis());

      return responseEntity;
    } catch (HttpStatusCodeException e) {
      if (stopWatch.isRunning()) {
        stopWatch.stop();
      }

      log.error(
          "class=AuditV1API method=publishAuditLog auditType={} message=\"Failed to send the audit log\" statusCode={} responseMessage={}",
          request.getAuditType(),
          e.getStatusCode(),
          e.getResponseBodyAsString());

      return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
    }
  }
}

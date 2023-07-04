package com.stubhub.identity.token.service.dye;

import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

@NoArgsConstructor
@Slf4j
public class DyeInterceptor implements ClientHttpRequestInterceptor {

  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    String dye = MDC.get(DyeConstants.DYE_KEY);

    if (!StringUtils.isEmpty(dye)) {
      request.getHeaders().add(DyeConstants.DYE_PATH, dye);
    }
    return execution.execute(request, body);
  }
}

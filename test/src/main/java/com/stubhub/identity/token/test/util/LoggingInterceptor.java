package com.stubhub.identity.token.test.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    logger.info(
        "message=sendRequest method={} URI={} headers={} body={}",
        request.getMethod(),
        request.getURI(),
        request.getHeaders(),
        new String(body, "UTF-8"));
    ClientHttpResponse response = execution.execute(request, body);
    logger.info(
        "message=receiveResponse status={} headers={} body={}",
        response.getStatusCode(),
        response.getHeaders(),
        new String(toByteArray(response.getBody()), "UTF-8"));
    return response;
  }

  public static byte[] toByteArray(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] bs = new byte[1024 * 2];
    int count = 0;
    while ((count = in.read(bs)) != -1) {
      out.write(bs, 0, count);
    }
    return out.toByteArray();
  }
}

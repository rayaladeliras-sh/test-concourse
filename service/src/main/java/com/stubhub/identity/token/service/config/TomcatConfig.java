package com.stubhub.identity.token.service.config;

import java.io.IOException;
import javax.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TomcatConfig {

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
    return (final TomcatServletWebServerFactory factory) -> {
      factory.addContextValves(
          new ValveBase() {
            @Override
            public void invoke(Request request, Response response)
                throws IOException, ServletException {
              final MessageBytes serverNameMB = request.getCoyoteRequest().serverName();
              String originalServerName = null;
              // GAE Router disregard X-Forwrded-Header, Akmai will set X-Forwarded-Server
              final String forwardedHost = request.getHeader("X-Forwarded-Server");
              if (forwardedHost != null) {
                log.debug("Override Host from X-Forwarded-Server:" + forwardedHost);
                originalServerName = serverNameMB.getString();
                serverNameMB.setString(forwardedHost);
              }

              try {
                getNext().invoke(request, response);
              } finally {
                if (forwardedHost != null) {
                  serverNameMB.setString(originalServerName);
                }
              }
            }
          });
    };
  }
}

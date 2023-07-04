package com.stubhub.identity.token.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@EnableSpringHttpSession
@EnableScheduling
@SpringBootApplication
public class TokenManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(TokenManagementApplication.class, args);
  }
}

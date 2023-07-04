package com.stubhub.identity.token.service.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  @GetMapping("/oauth/root")
  public String hello(Principal principal) {
    return index(principal);
  }

  @GetMapping("/")
  public String index(Principal principal) {
    if (null == principal) {
      return "Hello, you're not authenticated.";
    } else {
      return "Hello, " + principal.getName();
    }
  }
}

package com.stubhub.identity.token.service.controller;

import com.stubhub.identity.token.service.oidc.IDTokenService;
import com.stubhub.identity.token.service.oidc.UserClaimConstants;
import io.swagger.annotations.Api;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/oauth/v1")
public class UserController {
  @Autowired private IDTokenService idTokenService;

  @GetMapping("/user/me")
  public Map<String, String> me(Principal principal) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("name", principal.getName());
    return map;
  }

  @GetMapping("/userinfo")
  public Map<String, Object> user(Principal principal) {
    if (principal instanceof OAuth2Authentication) {
      return idTokenService.buildUserClaims((OAuth2Authentication) principal);
    } else {
      HashMap<String, Object> claims = new HashMap<>();
      claims.put(UserClaimConstants.USERNAME, principal.getName());
      return claims;
    }
  }
}

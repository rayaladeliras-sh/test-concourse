package com.stubhub.identity.token.service.controller;

import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/oauth/v1/.well-known")
public class WellKnownController {

  @Autowired private JWKSet jwkSet;

  @ApiOperation(value = "keys", notes = "return public key sets", httpMethod = "GET")
  @GetMapping("/jwks.json")
  public Map<String, Object> keys() {
    return this.jwkSet.toJSONObject();
  }
}

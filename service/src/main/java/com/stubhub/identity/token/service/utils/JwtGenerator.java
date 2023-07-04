package com.stubhub.identity.token.service.utils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.Signer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
public class JwtGenerator {

  @Autowired private KeyPair keyPair;

  private Signer signer;

  @PostConstruct
  public void init() {
    PrivateKey privateKey = keyPair.getPrivate();
    Assert.state(privateKey instanceof RSAPrivateKey, "KeyPair must be an RSA ");
    signer = new RsaSigner((RSAPrivateKey) privateKey);
  }

  public Jwt generateJWT(String content) {
    return JwtHelper.encode(content, signer);
  }
}

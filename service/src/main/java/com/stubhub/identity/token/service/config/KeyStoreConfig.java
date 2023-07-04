package com.stubhub.identity.token.service.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

@Slf4j
@Configuration
public class KeyStoreConfig {

  public static final String CLOUD_KEY_FILE = "identity-cloud.jks";
  public static final String CLOUD_KEY_ALIAS = "stubhub-cloud";
  public static final String CLOUD_KEY_PASSWORD = System.getProperty("JKS_TOKEN_PWD");

  @Bean
  public KeyPair keyPair() {
    KeyStoreKeyFactory factory =
        new KeyStoreKeyFactory(
            new ClassPathResource(CLOUD_KEY_FILE), CLOUD_KEY_PASSWORD.toCharArray());
    return factory.getKeyPair(CLOUD_KEY_ALIAS);
  }

  @Bean
  public RSAKey rsaKey() {
    return new RSAKey.Builder((RSAPublicKey) keyPair().getPublic())
        .keyUse(KeyUse.SIGNATURE)
        .algorithm(JWSAlgorithm.RS256)
        .keyID(CLOUD_KEY_ALIAS)
        .build();
  }

  @Bean
  public JWKSet jwkSet() {
    return new JWKSet(rsaKey());
  }

  @Bean
  public KeyStore keyStore()
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(
        new ClassPathResource(CLOUD_KEY_FILE).getInputStream(), CLOUD_KEY_PASSWORD.toCharArray());
    return keyStore;
  }
}

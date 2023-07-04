package com.stubhub.identity.token.test.util;

import java.util.Optional;

public class SHEnv {
  // TODO which url template???
  public static final String defaultEnv = "stubhubdev";

  public static String getEnv() {
    return Optional.ofNullable(System.getProperty("ENV")).orElse(defaultEnv);
  }

  public static String getEnvType() {
    return Optional.ofNullable(System.getProperty("ENV_TYPE"))
        .orElseThrow(() -> new RuntimeException("No ENV_TYPE value!"));
  }

  public static String getBaseURL() {
    return Optional.ofNullable(System.getProperty("BASE_URL"))
        .orElseThrow(() -> new RuntimeException("No BASE_URL value!"));
  }
}

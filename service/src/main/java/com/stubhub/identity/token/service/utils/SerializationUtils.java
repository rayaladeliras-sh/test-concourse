package com.stubhub.identity.token.service.utils;

import java.util.Base64;

public class SerializationUtils {

  public static String serialize(Object state) {
    return Base64.getEncoder()
        .encodeToString(
            org.springframework.security.oauth2.common.util.SerializationUtils.serialize(state));
  }

  public static <T> T deserialize(String byteString) {
    return org.springframework.security.oauth2.common.util.SerializationUtils.deserialize(
        Base64.getDecoder().decode(byteString));
  }
}

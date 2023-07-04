package com.stubhub.identity.token.service.utils;

import com.google.api.client.util.Charsets;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class ClientUtils {
  /**
   * Generates a random number using two UUIDs and HMAC-SHA1
   *
   * @return generated secure random number
   */
  public static String getRandomNumber() {
    try {
      String secretKey = UUID.randomUUID().toString();
      String baseString = UUID.randomUUID().toString();

      SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(Charsets.UTF_8), "HmacSHA1");
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(key);
      byte[] rawHmac = mac.doFinal(baseString.getBytes(Charsets.UTF_8));
      String random = Base64.encode(rawHmac);
      // Registry doesn't have support for these character.
      random = random.replace("/", "_");
      random = random.replace("=", "a");
      random = random.replace("+", "f");
      return random;
    } catch (Exception e) {
      return UUID.randomUUID().toString();
    }
  }
}

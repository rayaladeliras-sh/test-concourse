package com.stubhub.identity.token.service.utils;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

@RunWith(MockitoJUnitRunner.class)
public class SerializationUtilsTest {

  @Test
  public void testSerializeAndDeserialize() {
    HashMap<String, String> params = new HashMap<>();
    params.put("grant_type", "code");

    OAuth2Authentication oAuth2Authentication =
        new OAuth2Authentication(
            new OAuth2Request(
                params,
                "guest",
                null,
                true,
                Collections.emptySet(),
                Collections.EMPTY_SET,
                "",
                Collections.EMPTY_SET,
                null),
            new UsernamePasswordAuthenticationToken("test", "password"));

    String encode = SerializationUtils.serialize(oAuth2Authentication);
    OAuth2Authentication oAuth2Authentication2 = SerializationUtils.deserialize(encode);

    assertEquals(oAuth2Authentication2.getOAuth2Request().getGrantType(), "code");

    assertEquals(oAuth2Authentication2.getUserAuthentication().getPrincipal(), "test");
    assertEquals(oAuth2Authentication2.getUserAuthentication().getCredentials(), "password");
  }
}

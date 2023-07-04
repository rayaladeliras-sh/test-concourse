package com.stubhub.identity.token.test.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.test.util.SHRestAssured;
import com.stubhub.identity.token.test.util.TokenManagementAPI;
import java.util.*;
import lombok.SneakyThrows;
import org.testng.annotations.Test;

public class OidcIT extends BasicInit {

  @Test
  public void whenAuthorizeWithoutLogin_thenReturnLoginPage() {
    SHRestAssured.given()
        .when()
        .redirects()
        .follow(false)
        .get(BASE + "/authorize")
        .then()
        .statusCode(302)
        .header("Location", containsString("/identity/oauth/login"));
  }

  @Test
  public void whenAuthorizeWithWrongClient_thenReturn401() {
    String session = user.getSi();

    SHRestAssured.given()
        .cookie(TokenManagementAPI.SESSION_NAME, session)
        .param("response_type", "code")
        .param("client_id", "noclient")
        .param("redirect_uri", "http://stubhub.com/login")
        .param("scope", "openid")
        .param("state", UUID.randomUUID().toString())
        .when()
        .redirects()
        .follow(false)
        .get(BASE + "/authorize")
        .then()
        .statusCode(401);
  }

  @Test
  public void whenAuthorizeWithWrongRedirectUri_thenReturn400() {
    String session = user.getSi();

    SHRestAssured.given()
        .cookie(TokenManagementAPI.SESSION_NAME, session)
        .param("response_type", "code")
        .param("client_id", CLIENT_ID)
        .param("redirect_uri", "http://stubhub.com/no_register_uri")
        .param("scope", "openid")
        .param("state", UUID.randomUUID().toString())
        .when()
        .redirects()
        .follow(false)
        .get(BASE + "/authorize")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenAuthorizeWithWrongScope_thenReturnInvalidScope() {
    String session = user.getSi();

    SHRestAssured.given()
        .cookie(TokenManagementAPI.SESSION_NAME, session)
        .param("response_type", "code")
        .param("client_id", CLIENT_ID)
        .param("redirect_uri", "http://stubhub.com/login")
        .param("scope", "noscope")
        .param("state", UUID.randomUUID().toString())
        .when()
        .redirects()
        .follow(false)
        .get(BASE + "/authorize")
        .then()
        .statusCode(302)
        .header("Location", containsString("error=invalid_scope"));

    SHRestAssured.given()
        .cookie(TokenManagementAPI.SESSION_NAME, session)
        .param("response_type", "code")
        .param("client_id", CLIENT_ID)
        .param("redirect_uri", "http://stubhub.com/login")
        .param("scope", "openid, noscope")
        .param("state", UUID.randomUUID().toString())
        .when()
        .redirects()
        .follow(false)
        .get(BASE + "/authorize")
        .then()
        .statusCode(302)
        .header("Location", containsString("error=invalid_scope"));
  }

  @Test
  public void whenAuthorizeWithoutOpenIDScope_thenReturnNoIdToken() {
    Map<String, Map> response = oidcLoginFlow("default");
    assertThat(response.get("token").get("id_token"), nullValue());
  }

  @Test
  public void whenExchangeTokenWithDifferentClient_thenReturn401() {
    String session = user.getSi();
    String code = tokenManagementAPI.getAuthorizeCode(session, "guest", "openid");
    SHRestAssured.given()
        .contentType("application/json")
        .auth()
        .basic("test", "test")
        .when()
        .post(
            BASE
                + "/token?grant_type=authorization_code&redirect_uri=http://stubhub.com/login&code="
                + code)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenExchangeTokenWithWrongCode_thenReturn400() {
    String session = user.getSi();
    String code = tokenManagementAPI.getAuthorizeCode(session, CLIENT_ID, "openid");
    SHRestAssured.given()
        .contentType("application/json")
        .auth()
        .basic(CLIENT_ID, CLIENT_SECRET)
        .when()
        .post(
            BASE
                + "/token?grant_type=authorization_code&redirect_uri=http://stubhub.com/login&code="
                + code
                + "12")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenWrongTokenGetUserInfo_thenReturn401() {
    String access_token = "error token";
    SHRestAssured.given()
        .auth()
        .oauth2(access_token)
        .when()
        .get(BASE + "/userinfo")
        .then()
        .statusCode(401);

    SHRestAssured.given().when().get(BASE + "/userinfo").then().statusCode(401);
  }

  @Test
  public void whenGivenOpenidScope_thenReturnIdToken() {
    Map<String, Map> response = oidcLoginFlow("openid");

    Map userInfo = response.get("user");
    Set<String> claims = userInfo.keySet();
    assertThat(claims, hasItem("sub"));
    assertThat(claims, hasItem("username"));

    Map<String, Object> expectedValueMap = new HashMap<>();
    //    expectedValueMap.put("client_id", CLIENT_ID);
    expectedValueMap.put("username", userInfo.get("username"));
    expectedValueMap.put("sub", userInfo.get("sub"));
    assertThat(response.get("token").get("id_token").toString(), not(isEmptyString()));
    checkIdToken(response.get("token").get("id_token").toString(), expectedValueMap);
  }

  @Test
  public void whenGivenProfileScope_thenReturnProfile() {
    Map<String, Map> response = oidcLoginFlow("openid profile");
    Map userInfo = response.get("user");
    Set<String> claims = userInfo.keySet();
    assertThat(claims, hasItem("sub"));
    assertThat(claims, hasItem("username"));
    assertThat(claims, hasItem("name"));
    assertThat(claims, hasItem("given_name"));

    String idToken = response.get("token").get("id_token").toString();
    Map<String, Object> expectedValueMap = new HashMap<>();
    expectedValueMap.put("name", userInfo.get("name"));
    expectedValueMap.put("given_name", userInfo.get("given_name"));
    checkIdToken(idToken, expectedValueMap);
  }

  @Test
  public void whenGivenAddressScope_thenReturnAddress() {
    Map<String, Map> response = oidcLoginFlow("openid address");
    Map userInfo = response.get("user");
    Set<String> claims = userInfo.keySet();
    assertThat(claims, hasItem("sub"));
    assertThat(claims, hasItem("username"));
    assertThat(claims, hasItem("phone"));
  }

  //  no group for site users
  //  @Test
  //  public void whenGivenGroupsScope_thenReturnGroups() {
  //    Map<String, Map> response = oidcLoginFlow("openid groups");
  //    Map userInfo = response.get("user");
  //    Set<String> claims = userInfo.keySet();
  //    assertThat(claims, hasItem("sub"));
  //    assertThat(claims, hasItem("username"));
  //    assertThat(claims, hasItem("authorities"));
  //
  //    String idToken = response.get("token").get("id_token").toString();
  //    Map<String, Object> expectedValueMap = new HashMap<>();
  //    expectedValueMap.put("authorities", userInfo.get("authorities"));
  //    checkIdToken(idToken, expectedValueMap);
  //  }

  @Test
  public void whenGivenEmailScope_thenReturnEmail() {
    Map<String, Map> response = oidcLoginFlow("openid email");
    Map userInfo = response.get("user");
    Set<String> claims = userInfo.keySet();
    assertThat(claims, hasItem("sub"));
    assertThat(claims, hasItem("username"));
    assertThat(claims, hasItem("email"));

    String idToken = response.get("token").get("id_token").toString();
    Map<String, Object> expectedValueMap = new HashMap<>();
    expectedValueMap.put("email", user.getEmail().toLowerCase());
    checkIdToken(idToken, expectedValueMap);
  }

  @Test
  public void whenLogout_thenSessionIsCleared() {
    String session = user.getSi();
    tokenManagementAPI.logout(session);
    whenAuthorizeWithoutLogin_thenReturnLoginPage();
  }

  @SneakyThrows
  public HashMap checkIdToken(String idToken, Map expectedValueMap) {
    ObjectMapper objectMapper = new ObjectMapper();

    String[] parts = idToken.split("\\.");
    String body = new String(Base64.getUrlDecoder().decode(parts[1]));
    HashMap bodyMap = objectMapper.readValue(body, HashMap.class);
    expectedValueMap.forEach(
        (k, v) -> {
          assertEquals("Should Match Expected Value !!", v, bodyMap.get(k));
        });
    return bodyMap;
  }
}

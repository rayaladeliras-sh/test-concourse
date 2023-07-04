package com.stubhub.identity.token.test.util;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import com.stubhub.identity.token.test.dto.InnerIssueTokenDto;
import com.stubhub.identity.token.test.dto.SHCookieDto;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@Slf4j
public class TokenManagementAPI {

  private String tokenServerEndpoint;
  public static final String SESSION_NAME = "SH_SI";

  public TokenManagementAPI(String url) {
    tokenServerEndpoint = url;
  }

  private RequestSpecification given() {
    RequestSpecification specification =
        new RequestSpecBuilder()
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .setRelaxedHTTPSValidation()
            .setBaseUri(tokenServerEndpoint)
            .build();

    return RestAssured.given().spec(specification);
  }

  public OAuth2AccessToken getAppToken(String clientId, String clientSecret) {

    return given()
        .auth()
        .basic(clientId, clientSecret)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
        .post("/v1/token?grant_type=client_credentials")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .as(OAuth2AccessToken.class);
  }

  /**
   * @param clientId which has scope can issue user token
   * @param clientSecret the password related to the client id
   */
  public OAuth2AccessToken getUserToken(
      String clientId, String clientSecret, InnerIssueTokenDto innerIssueTokenDto) {

    return given()
        .auth()
        .oauth2(getAppToken(clientId, clientSecret).getValue())
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(innerIssueTokenDto)
        .when()
        .post("/v1/ext/token")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .as(OAuth2AccessToken.class);
  }

  public String getExchangeToken(SHCookieDto cookieDto) {

    return given()
        .contentType("application/json")
        .accept("application/json")
        .body(cookieDto)
        .when()
        .post("/v1/ext/extoken")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .path("jwt")
        .toString();
  }

  // return access token
  public String refreshToken(String clientId, String clientSecret, String refreshToken) {
    // use the client id which issue the origin refresh token
    return given()
        .auth()
        .basic(clientId, clientSecret)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .when()
        .post("/v1/token?grant_type=refresh_token&refresh_token=" + refreshToken)
        .then()
        .statusCode(200)
        .body("access_token", not(empty()))
        .body("refresh_token", not(empty()))
        .rootPath("access_token")
        .extract()
        .body()
        .path("access_token")
        .toString();
  }

  // return refresh token
  public String refreshTokenEx(String clientId, String clientSecret, String refreshToken) {
    // use the client id which issue the origin refresh token
    return given()
        .auth()
        .basic(clientId, clientSecret)
        .when()
        .post("/v1/token?grant_type=refresh_token&refresh_token=" + refreshToken)
        .then()
        .statusCode(200)
        .body("access_token", not(empty()))
        .body("refresh_token", not(empty()))
        .rootPath("access_token")
        .extract()
        .body()
        .path("refresh_token")
        .toString();
  }

  public String login(String username, String password) {
    // login
    return given()
        .config(
            RestAssured.config()
                .encoderConfig(
                    encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.TEXT)))
        .formParam("username", username)
        .formParam("password", password)
        .when()
        .post("/login")
        .then()
        .statusCode(302)
        .cookie(SESSION_NAME, notNullValue())
        .header("Location", tokenServerEndpoint + "/")
        .extract()
        .cookie(SESSION_NAME);
  }

  public void logout(String session) {
    ExtractableResponse response =
        given().cookie(SESSION_NAME, session).when().get("/logout").then().extract();
    Map cookies = response.cookies();
    assertThat(cookies.get(response.sessionId()), nullValue());
    assertThat(response.statusCode(), equalTo(200));
    // System.out.println(response);
  }

  public String getAuthorizeCode(String session, String clientId, String scope) {
    // get authorize code
    String state = UUID.randomUUID().toString();
    String location =
        given()
            .cookie(SESSION_NAME, session)
            .param("response_type", "code")
            .param("client_id", clientId)
            .param("redirect_uri", "http://stubhub.com/login")
            .param("scope", scope)
            .param("state", state)
            .when()
            .redirects()
            .follow(false)
            .get("/v1/authorize")
            .then()
            .statusCode(302)
            .header("Location", containsString("code="))
            .extract()
            .header("Location");

    String code = null;
    try {
      Map<String, String> params = splitQuery(new URL(location));
      code = params.get("code");
      assertThat(code, notNullValue());
      String retState = params.get("state");
      assertThat(state, equalTo(retState));
    } catch (UnsupportedEncodingException | MalformedURLException e) {
      assert false;
      e.printStackTrace();
    }

    return code;
  }

  private static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    String query = url.getQuery();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(
          URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return query_pairs;
  }

  public Map exchangeCode4Token(String code, String clientId, String clientSecret) {
    // exchange token
    HashMap body =
        given()
            .contentType("application/json")
            .auth()
            .basic(clientId, clientSecret)
            .when()
            .post(
                "/v1/token?grant_type=authorization_code&redirect_uri=http://stubhub.com/login&code="
                    + code)
            .then()
            .statusCode(200)
            .body("token_type", equalTo("bearer"))
            .extract()
            .body()
            .as(HashMap.class);
    return body;
  }

  public Map getUserInfo(String token) {
    return given()
        .auth()
        .oauth2(token)
        .when()
        .get("/v1/userinfo")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .as(HashMap.class);
  }

  public void checkToken(String token, Map<String, Object> expectedValueMap) {
    Map<String, Object> valueMap =
        given()
            .when()
            .get("/v1/check_token?token=" + token)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(Map.class);
    expectedValueMap.forEach(
        (k, v) -> {
          assertEquals("Should Match Expected Value !!", v, valueMap.get(k));
        });
  }

  public void checkToken(String token, String clientId, String iss) {
    given()
        .when()
        .get("/v1/check_token?token=" + token)
        .then()
        .statusCode(200)
        .body("client_id", equalTo(clientId))
        .body("iss", equalTo(iss));
  }

  public HashMap getClaims(String token) {
    log.info("request token = {}", token);
    HashMap ret =
        given()
            .when()
            .get("/v1/check_token?token=" + token)
            .then()
            .statusCode(200)
            .extract()
            .as(HashMap.class);
    log.info("response map = {}", ret);
    return ret;
  }
}

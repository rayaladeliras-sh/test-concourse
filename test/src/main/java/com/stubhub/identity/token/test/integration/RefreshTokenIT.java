package com.stubhub.identity.token.test.integration;

import com.stubhub.identity.token.test.dto.InnerIssueTokenDto;
import com.stubhub.identity.token.test.util.SHRestAssured;
import com.stubhub.identity.token.test.util.ShapeAPI;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.testng.annotations.Test;

@Slf4j
public class RefreshTokenIT extends BasicInit {

  private final String BFE_NATIVE_CLIENT_ID = "_FxXELf4OfALNv_OaYULPy88ofca";
  private final String BFE_NATIVE_CLIENT_SECRET = "6AZQjHV6wNLpBoCGWnwLyraHIvsa";
  private final String GUEST_CLIENT_ID = "guest";
  private final String GUEST_CLIENT_SECRET = "guest";

  public RefreshTokenIT() {
    super();
  }

  @Test
  public void whenGivenCloudRefreshTokenRefreshTwice_thenReturn401() {
    String refreshToken = cloudRefreshToken();

    String newToken =
        tokenManagementAPI.refreshTokenEx(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, refreshToken);

    // refresh again with origin token
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + refreshToken)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenRightClientRefreshToken_thenReturn200() {
    String refreshToken = cloudRefreshToken();
    int i = 5;
    while (i-- > 0) {
      String newRefreshToken =
          tokenManagementAPI.refreshTokenEx(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, refreshToken);
      refreshToken = newRefreshToken;
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        log.error("thread sleep error: {}", e.getLocalizedMessage());
      }
      log.info("new refresh token: {}", refreshToken);
    }

    String newToken =
        tokenManagementAPI.refreshToken(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, refreshToken);
    tokenManagementAPI.checkToken(newToken, GUEST_CLIENT_ID, CLAIMS_ISS);
  }

  @Test
  public void whenGivenWrongClientRefreshToken_thenReturn400() {

    // use different client id should return error
    SHRestAssured.given()
        .auth()
        .basic("test", "test")
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + cloudRefreshToken())
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenExpiredJwtRefreshToken_thenReturn401() {
    String expiredRefreshToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiI5NjdCRDk3MzE2Q0ZBMDcyRTA1NDAwMTBFMDVENTlFMCIsImF1ZCI6WyJhcGk6Ly9zdHViaHViIl0sInVzZXJfbmFt"
            + "ZSI6ImlkZW50aXR5XzlpZDJvMG5qcHdmYnQ2dmpAc2h0ZXN0LmNvbSIsInNjb3BlIjpbImlkZW50aXR5OnRtZ3Q6ZXh0OnRva2VuIi"
            + "wiZGVmYXVsdCIsImFkZHJlc3MiLCJvcGVuaWQiLCJwcm9maWxlIiwiZ3JvdXBzIiwiZW1haWwiXSwiYXRpIjoiYWQ5YTA1N2ItZmNiN"
            + "y00NmNiLWI5Y2YtZjlmMGI0M2JmNDRkIiwiaXNzIjoiaHR0cHM6Ly9hcGMuc3R1Ymh1Yi5jb20vaWRlbnRpdHkvb2F1dGgiLCJleHAiO"
            + "jE1NzMyMDg1MTUsImlhdCI6MTU3MzIwNDkxNSwianRpIjoiNDcxN2VkNGYtYzA0MS00YzM3LTliODctZGJjZmZkNjQzNj"
            + "IwIiwiY2xpZW50X2lkIjoiZ3Vlc3QifQ."
            + "PQOtSq3iLvsSBLN_zVUQkQEBDthE0zYLOVhLDDV9s74uXShYP11rGhNZToAiD39Ys0G3eT3WH_toysyFZicrYjEzbX8QydCNWHxsFz8ZhP"
            + "2GUPMPbI-zyWJ9vqWYi-D0uZfDiIpQNTaazecVheXCoEdgV5G5P9q1ajKrulZMFUWnjRvvSmVVm3r0a1djw_N5jkj5KAKXQUKAK3hzoZ3U"
            + "wv71kHaxa4nIWm13Ndl0pNvQYwaatGsF4BujsObbGdqEFhDxCX3Kl4XOKICG33tCy_ELPV3YJ8XCBN4RkM-fs0Sfp1Egc3RYH108hoW"
            + "oaOLHyrel8dxEAZhKvBSc0PRrow";
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + expiredRefreshToken)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenLegacyRefreshToken_thenReturn200() {
    String legacyToken = legacyRefreshToken();

    // valid client
    String newToken =
        tokenManagementAPI.refreshToken(
            BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET, legacyToken);

    tokenManagementAPI.checkToken(newToken, BFE_NATIVE_CLIENT_ID, CLAIMS_ISS);
  }

  @Test
  public void whenGivenLegacyRefreshTokenWithWrongClient_thenReturn401() {
    String legacyToken = legacyRefreshToken();

    // invalid client
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + legacyToken)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenNonExistUserRefreshToken_thenReturn401() {

    // issue user token and refresh token with non-exist user
    // why it works, because if use email and guid to issue user token will not check user validity
    String refreshToken =
        cloudRefreshToken(
            InnerIssueTokenDto.builder()
                .clientId(GUEST_CLIENT_ID)
                .guid("1111111111111")
                .email("non@stubhub.com")
                .build());
    // refresh immediately should ok
    //    String newToken =
    //        tokenManagementAPI.refreshTokenEx(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, refreshToken);

    //     sleep 32s assure user cache is expired
    try {
      Thread.sleep(50000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // refresh again after user cache expire, expire time current is 30s
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + refreshToken)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenLegacyRefreshTokenRefreshLegacyTokenTwice_thenReturn401() {
    String legacyToken = legacyRefreshToken();

    String newToken =
        tokenManagementAPI.refreshTokenEx(
            BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET, legacyToken);

    // refresh again with origin token
    SHRestAssured.given()
        .auth()
        .basic(BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=refresh_token&refresh_token=" + legacyToken)
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenLegacyRefreshTokenRefreshNewTokenTwice_thenReturn200() {
    String legacyToken = legacyRefreshToken();

    String newToken =
        tokenManagementAPI.refreshTokenEx(
            BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET, legacyToken);

    // refresh again with new token
    tokenManagementAPI.refreshTokenEx(BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET, newToken);
  }

  @Test
  public void whenGivenRightParamsRevokeRefreshToken_thenReturn200() {
    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=" + cloudRefreshToken())
        .then()
        .statusCode(200);
  }

  @Test
  public void whenGivenRightParamsRevokeRefreshTokenTwice_thenReturn200() {
    String refreshToken = cloudRefreshToken();
    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=" + refreshToken)
        .then()
        .statusCode(200);

    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=" + refreshToken)
        .then()
        .statusCode(200);
  }

  @Test
  public void whenGivenWrongCredentialsRevokeRefreshToken_thenReturn401() {
    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, "")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=")
        .then()
        .statusCode(401);

    SHRestAssured.given()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=")
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGivenWrongTypeRevokeRefreshToken_thenReturn400() {
    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=&token_type_hint=access_token")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenWrongTokenRevokeRefreshToken_thenReturn200() {
    SHRestAssured.given()
        .auth()
        .preemptive()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .when()
        .post(BASE + "/ext/token/revoke?token=sdfsdfsf")
        .then()
        .statusCode(200);
  }

  private String cloudRefreshToken() {

    return SHRestAssured.given()
        .auth()
        .oauth2(getAppToken(""))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{" + "\"clientId\":\"guest\"," + "\"guid\":\"" + user.getGuid() + "\"" + "}")
        .when()
        .post(BASE + "/ext/token")
        .then()
        .statusCode(200)
        .rootPath("refresh_token")
        .extract()
        .body()
        .path("refresh_token")
        .toString();
  }

  private String cloudRefreshToken(InnerIssueTokenDto innerIssueTokenDto) {
    return SHRestAssured.given()
        .auth()
        .oauth2(getAppToken(""))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(innerIssueTokenDto)
        .when()
        .post(BASE + "/ext/token")
        .then()
        .statusCode(200)
        .rootPath("refresh_token")
        .extract()
        .body()
        .path("refresh_token")
        .toString();
  }

  private String legacyRefreshToken() {
    return ShapeAPI.token(
            user.getEmail(), user.getPassword(), BFE_NATIVE_CLIENT_ID, BFE_NATIVE_CLIENT_SECRET)
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .as(OAuth2AccessToken.class)
        .getRefreshToken()
        .getValue();
  }
}

package com.stubhub.identity.token.test.integration;

import static org.hamcrest.Matchers.*;

import com.stubhub.identity.token.test.dto.InnerIssueTokenDto;
import com.stubhub.identity.token.test.util.SHRestAssured;
import com.stubhub.identity.token.test.util.TestDataUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.testng.annotations.Test;

@Slf4j
public class TokenControllerIT extends BasicInit {

  private final String GUEST_CLIENT_ID = "guest";
  private final String GUEST_CLIENT_SECRET = "guest";

  public TokenControllerIT() {
    super();
  }

  @Test
  public void whenSendingTokenKey_thenMessageIsReturned() {
    SHRestAssured.given().when().get(BASE + "/token_key").then().statusCode(200);
  }

  @Test
  public void whenSendingTokenKeySet_thenMessageIsReturned() {
    SHRestAssured.given()
        .when()
        .get(BASE + "/.well-known/jwks.json")
        .then()
        .statusCode(200)
        .body("keys.kid", hasItems("stubhub-cloud"));
  }

  @Test
  public void whenGiveWrongClientCredential_thenReturn401() {
    SHRestAssured.given()
        .auth()
        .basic("guest1", GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=client_credentials")
        .then()
        .statusCode(401);

    SHRestAssured.given()
        .auth()
        .basic("guest1", "guest2")
        .when()
        .post(BASE + "/token?grant_type=client_credentials")
        .then()
        .statusCode(401);
  }

  @Test
  public void whenGiveRightScope_thenReturn200() {
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=client_credentials&scope=default")
        .then()
        .statusCode(200)
        .body("scope", equalTo("default"));
  }

  @Test
  public void whenGiveWrongScope_thenReturn400() {
    SHRestAssured.given()
        .auth()
        .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
        .when()
        .post(BASE + "/token?grant_type=client_credentials&scope=none")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGiveRightClientCredential_thenReturn200() {
    String token =
        SHRestAssured.given()
            .auth()
            .basic(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET)
            .when()
            .post(BASE + "/token?grant_type=client_credentials")
            .then()
            .statusCode(200)
            .body("token_type", equalTo("bearer"))
            .rootPath("access_token")
            .extract()
            .body()
            .path("access_token")
            .toString();

    Map<String, Object> expectedValueMap = new HashMap<>();
    expectedValueMap.put("client_id", GUEST_CLIENT_ID);
    expectedValueMap.put("iss", CLAIMS_ISS);
    tokenManagementAPI.checkToken(token, expectedValueMap);
  }

  @Test
  public void whenIssueUserToken_thenReturn200() {

    String userToken =
        SHRestAssured.given()
            .auth()
            .oauth2(getAppToken("identity:tmgt:ext:token"))
            .contentType("application/json")
            .body("{" + "\"clientId\":\"guest\"," + "\"guid\":\"" + user.getGuid() + "\"" + "}")
            .when()
            .post(BASE + "/ext/token")
            .then()
            .statusCode(200)
            .body("token_type", equalTo("bearer"))
            .body("access_token", not(empty()))
            .body("refresh_token", not(empty()))
            .rootPath("access_token")
            .extract()
            .body()
            .path("access_token")
            .toString();

    SHRestAssured.given()
        .when()
        .get(BASE + "/check_token?token=" + userToken)
        .then()
        .statusCode(200)
        .body("client_id", equalTo(GUEST_CLIENT_ID))
        .body("sub", equalTo(user.getGuid()));
  }

  @Test
  public void whenIssueUserTokenAndRefreshToken_thenReturn200() {

    String refreshToken =
        SHRestAssured.given()
            .auth()
            .oauth2(getAppToken(""))
            .contentType("application/json")
            .body(
                "{\n"
                    + "\t\"clientId\":\"guest\",\n"
                    + "\t\"guid\":\""
                    + user.getGuid()
                    + "\",\n"
                    + "\t\"email\":\""
                    + user.getEmail()
                    + "\"\n"
                    + "}")
            .when()
            .post(BASE + "/ext/token")
            .then()
            .statusCode(200)
            .body("token_type", equalTo("bearer"))
            .body("access_token", not(empty()))
            .body("refresh_token", not(empty()))
            .rootPath("refresh_token")
            .extract()
            .body()
            .path("refresh_token")
            .toString();

    String newToken =
        tokenManagementAPI.refreshToken(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, refreshToken);
    tokenManagementAPI.checkToken(newToken, "guest", CLAIMS_ISS);
  }

  @Test
  public void whenGivenWrongScopeIssueUserToken_thenReturn403() {
    SHRestAssured.given()
        .auth()
        .oauth2(getAppToken("default"))
        .contentType("application/json")
        .body("{" + "\"clientId\":\"guest\"," + "\"guid\":\"" + user.getGuid() + "\"" + "}")
        .when()
        .post(BASE + "/ext/token")
        .then()
        .statusCode(403);
  }

  @Test
  public void whenGivenNoneExistGuidAndEmailIssueUserToken_thenReturn200() {

    // passing guid and email can issue user token directly
    // check user here, can find user by guid but can not find by email
    SHRestAssured.given()
        .auth()
        .oauth2(getAppToken(""))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
            InnerIssueTokenDto.builder()
                .clientId(GUEST_CLIENT_ID)
                .guid("111111222211")
                .email("no@email.com")
                .build())
        .when()
        .post(BASE + "/ext/token")
        .then()
        .statusCode(200);
  }

  @Test
  public void whenGivenRightGuidWithWrongEmailIssueUserToken_thenReturn401() {
    TestDataUtil.LoginUser user2 = TestDataUtil.UserGenerator.defaultMeta().ready().create();

    // not check user here
    // passing guid and email can issue user token directly
    SHRestAssured.given()
        .auth()
        .oauth2(getAppToken(""))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(InnerIssueTokenDto.builder().clientId(GUEST_CLIENT_ID).guid(user2.getGuid()).build())
        .when()
        .post(BASE + "/ext/token")
        .then()
        .statusCode(200);

    // todo
    // can not find user by email in the cache in this case
    //    SHRestAssured.given()
    //        .auth()
    //        .oauth2(getAppToken(""))
    //        .contentType(MediaType.APPLICATION_JSON_VALUE)
    //        .body(
    //            InnerIssueTokenDto.builder()
    //                .clientId(GUEST_CLIENT_ID)
    //                .guid(user2.getGuid())
    //                .email("noxxx@email.com")
    //                .build())
    //        .when()
    //        .post(BASE + "/ext/token")
    //        .then()
    //        .statusCode(401);
  }
}

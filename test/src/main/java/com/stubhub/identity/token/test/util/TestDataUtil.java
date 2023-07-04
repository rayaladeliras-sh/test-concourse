package com.stubhub.identity.token.test.util;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

public class TestDataUtil {

  public static final String GUS_URL = "http://gus.stubcorp.com";

  public static final String QA_SH_AT =
      "nMvE43MFG6ykXcyiZ3WykYiTPItvEMgHZthsINTxHezmhIYcDJkXXHy%2BG%2BzoZz5vPTtwlgz85PCMnc%2B7B%2BAs%2BuYMZJg9ZxalUnBscO7UIWY%3D";

  private static RequestSpecification GusGiven() {
    RequestSpecification specification =
        new RequestSpecBuilder()
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .setRelaxedHTTPSValidation()
            .setBaseUri(GUS_URL) // https://github.com/rest-assured/rest-assured/issues/508
            .build();

    return RestAssured.given().spec(specification);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class LoginUser {
    private String email;
    private String password;
    private String guid;
    private String jwtToken;
    private String csrfToken;
    private String si;
    private String sh_ut;
    private String sh_at;
  }

  @Builder(builderMethodName = "defaultMeta", buildMethodName = "ready")
  public static class UserGenerator {
    @Builder.Default private String env = SHEnv.getEnv();
    @Builder.Default private String country = "US";

    public LoginUser create() {
      LoginUser user = new LoginUser();
      user.setEmail("identity_" + RandomStringUtils.randomAlphanumeric(16) + "@shtest.com");
      user.setPassword("Cool@Identity");
      // enrich GUID
      String guid = ShapeAPI.createUser(user.getEmail(), user.getPassword());
      user.setGuid(guid);

      String consumerKey = "iXyhYOR6pJl9TtYyYSzoiBXiB6sa";
      String consumerSecret = "6_9n4Hqz7j8jGQ0P6A6gjcGWXCka";
      String tempRefId = String.valueOf(System.currentTimeMillis() % 65536);
      // prepare to get jwt token : sessionId
      // enrich CSRF
      ExtractableResponse loginResp = login(user, consumerKey, consumerSecret, tempRefId);
      String loginSessionId = loginResp.path("login.session_id");
      String csrf_token = loginResp.path("login.csrf_token");
      user.setSi(loginSessionId);
      user.setCsrfToken(csrf_token);
      // prepare to get jwt token :sh_ut
      String loginSH_UT = getSHUTFromCookie(loginSessionId);
      user.setSh_ut(loginSH_UT);
      // prepare to get jwt token :sh_at
      String cookieSH_AT = getSHATFromCookie(loginSessionId);
      user.setSh_at(cookieSH_AT);
      // get jwt token
      //      String loginJWTToken = getJwtToken(loginSH_UT);
      //
      //      user.setJwtToken(loginJWTToken);

      return user;
    }
  }

  private static String getJwtToken(String loginSH_UT) {
    return RestAssured.given()
        .spec(
            new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .setRelaxedHTTPSValidation()
                .build())
        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
        .contentType(ContentType.JSON)
        .body("{" + "\"cookie\":\"" + loginSH_UT + "\"}")
        .post(String.format("https://www.%s.com/shape/oauth/extoken/v1", SHEnv.getEnv()))
        .then()
        .contentType(ContentType.JSON)
        .extract()
        .path("jwt");
  }

  private static String getSHUTFromCookie(String loginSessionId) {
    return RestAssured.given()
        .spec(
            new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .setRelaxedHTTPSValidation()
                .build())
        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
        .header(
            "X-SH-Service-Context",
            "{role=R1, operatorId=identity-user-integration-test, proxiedId=test}")
        .post(
            String.format(
                "https://api-shape.%s.com/iam/session/token/init?si=" + loginSessionId,
                SHEnv.getEnv()))
        .cookie("SH_UT");
  }

  // TODO
  private static String getSHATFromCookie(String loginSessionId) {
    return null;
  }

  public static ExtractableResponse<Response> login(
      LoginUser user, String consumerKey, String consumerSecret, String tempRefId) {
    return RestAssured.given()
        .spec(
            new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .setRelaxedHTTPSValidation()
                .build())
        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
        .header(
            "X-SH-Service-Context",
            "{role=R1, operatorId=identity-user-integration-test, proxiedId=test}")
        .post(
            String.format(
                "https://api-shape.%s.com/iam/login?username=%s&password=%s&consumer_key=%s&consumer_secret=%s&tmRefID=%s",
                SHEnv.getEnv(),
                user.getEmail(),
                user.getPassword(),
                consumerKey,
                consumerSecret,
                tempRefId))
        .then()
        .contentType(ContentType.JSON)
        .extract();
  }

  //  private static String getGUID(User user) {
  //    return SHRestAssured.given()
  //        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
  //        .header(
  //            "X-SH-Service-Context",
  //            "{role=R1, operatorId=identity-user-integration-test, proxiedId=test}")
  //        .get(
  //            String.format("https://api-int.%s.com/user/customers/v2/{id}/guid", SHEnv.getEnv()),
  //            user.getId())
  //        .then()
  //        .contentType(ContentType.JSON)
  //        .extract()
  //        .path("customer.userCookieGuid");
  //  }
}

package com.stubhub.identity.token.test.integration;

import com.stubhub.identity.token.test.dto.InnerIssueTokenDto;
import com.stubhub.identity.token.test.util.ShapeAPI;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Slf4j
public class UserCustomerIT extends BasicInit {

  public UserCustomerIT() {
    super();
  }

  public static RequestSpecification given() {
    RequestSpecification specification =
        new RequestSpecBuilder()
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .setRelaxedHTTPSValidation()
            .setBaseUri(ShapeAPI.getBase())
            .build();

    return RestAssured.given().spec(specification);
  }

  private OAuth2AccessToken getAppToken() {
    return tokenManagementAPI.getAppToken(
        "iySkCfogdLpp4LNIX1PdqK6gx0wa", "CE3sPlYasKALmdfW9iQ8JAyCX6ga");
  }

  private OAuth2AccessToken getUserToken(InnerIssueTokenDto innerIssueTokenDto) {
    return tokenManagementAPI.getUserToken(
        "iySkCfogdLpp4LNIX1PdqK6gx0wa", "CE3sPlYasKALmdfW9iQ8JAyCX6ga", innerIssueTokenDto);
  }

  private String getToken4WebUI() {
    return getUserToken(
            InnerIssueTokenDto.builder()
                .clientId("iySkCfogdLpp4LNIX1PdqK6gx0wa")
                .guid(user.getGuid())
                .build())
        .getValue();
  }

  private String getToken4Native() {
    return getUserToken(
            InnerIssueTokenDto.builder()
                .clientId("_FxXELf4OfALNv_OaYULPy88ofca")
                .guid(user.getGuid())
                .build())
        .getValue();
  }

  @Test
  public void testChangePassword() {
    String path = "/user/customers/v1/" + user.getGuid() + "?action=changePassword";
    String body =
        "{\n"
            + " \"customer\": {\n"
            + "\"oldPassword\": \""
            + user.getPassword()
            + "\",\n"
            + "\"newPassword\": \"Change!It\"\n"
            + " } \n"
            + "}";

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getAppToken())
        .body(body)
        .when()
        .put(path)
        .then()
        .statusCode(403);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4WebUI())
        .body(body)
        .when()
        .put(path)
        .then()
        .statusCode(204);

    String nativeToken = getToken4Native();

    System.out.println("user password:" + user.getPassword());
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + nativeToken)
        .body(
            "{\n"
                + " \"customer\": {\n"
                + "\"oldPassword\": \"Change!It\",\n"
                + "\"newPassword\": \""
                + user.getPassword()
                + "\"\n"
                + " } \n"
                + "}")
        .when()
        .put(path)
        .then()
        .statusCode(204);
  }

  @Test
  @Ignore
  public void testForgetPassword() {
    String path = "/user/customers/v1/?action=forgotPassword";

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getAppToken())
        .body(
            " {\n"
                + " \"customer\": {\n"
                + " \"emailAddress\": \""
                + user.getEmail()
                + "\",\n"
                + "\n"
                + " \"platform\": \"WEB\"\n"
                + "\n"
                + " } \n"
                + "}")
        .when()
        .put(path)
        .then()
        .statusCode(204);

    // for web client, it need csrf token
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4WebUI())
        .body(
            " {\n"
                + " \"customer\": {\n"
                + " \"emailAddress\": \""
                + user.getEmail()
                + "\",\n"
                + "\n"
                + " \"platform\": \"WEB\"\n"
                + "\n"
                + " } \n"
                + "}")
        .when()
        .put(path)
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4Native())
        .body(
            " {\n"
                + " \"customer\": {\n"
                + " \"emailAddress\": \""
                + user.getEmail()
                + "\",\n"
                + "\n"
                + " \"platform\": \"MOBILE\"\n"
                + "\n"
                + " } \n"
                + "}")
        .when()
        .put(path)
        .then()
        .statusCode(204);
  }

  @Test
  public void testGetCustomerByEmail() {
    String path = "user/customers/v1/?action=checkEmail&emailAddress=" + user.getEmail();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getAppToken())
        .when()
        .get(path)
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4WebUI())
        .when()
        .get(path)
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4Native())
        .when()
        .get(path)
        .then()
        .statusCode(204);
  }

  @Test
  public void testPasswordStrength() {
    String path = "/user/customers/v1/?action=passwordStrength&password=abcd1234";

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getAppToken())
        .when()
        .get(path)
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4WebUI())
        .when()
        .get(path)
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getToken4Native())
        .when()
        .get(path)
        .then()
        .statusCode(200);
  }
}

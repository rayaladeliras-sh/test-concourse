package com.stubhub.identity.token.test.integration;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import com.stubhub.identity.token.test.dto.SHCookieDto;
import com.stubhub.identity.token.test.util.SHRestAssured;
import com.stubhub.identity.token.test.util.ShapeAPI;
import com.stubhub.identity.token.test.util.TestDataUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Slf4j
public class ExtokenIT extends BasicInit {

  public ExtokenIT() {
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

  @Test
  public void whenGivenRightCookieExchangeToken_thenReturn200() {
    String SH_UT = user.getSh_ut();
    SHCookieDto cookieDto = SHCookieDto.builder().cookie(SH_UT).build();
    String userToken =
        SHRestAssured.given()
            .contentType("application/json")
            .body(cookieDto)
            .when()
            .post(BASE + "/ext/extoken")
            .then()
            .statusCode(200)
            .body("jwt", not(empty()))
            .extract()
            .body()
            .path("jwt")
            .toString();
    SHRestAssured.given()
        .when()
        .get(BASE + "/check_token?token=" + userToken)
        .then()
        .statusCode(200);
  }

  @Test
  public void whenGivenWrongCookieExchangeToken_thenReturn400() {
    SHCookieDto cookieDto = SHCookieDto.builder().cookie("adfsdvasdf").build();
    SHRestAssured.given()
        .contentType("application/json")
        .body(cookieDto)
        .when()
        .post(BASE + "/ext/extoken")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenNullBodyRequest_thenReturn400() {
    SHCookieDto cookieDto = SHCookieDto.builder().build();
    SHRestAssured.given()
        .contentType("application/json")
        .body(cookieDto)
        .when()
        .post(BASE + "/ext/extoken")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenDecryptFailedCookie_thenReturn400() {
    String decryptFailedCookie = "FZq%2F5laeFtEmefKzuWyNHA%3D%3D";
    SHCookieDto cookieDto = SHCookieDto.builder().cookie(decryptFailedCookie).build();
    SHRestAssured.given()
        .contentType("application/json")
        .body(cookieDto)
        .when()
        .post(BASE + "/ext/extoken")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGivenRightTokenExchangeToken_thenReturn200() {

    SHCookieDto cookieDto = SHCookieDto.builder().token("JYf0azPrf1RAvhUhpGZudVU9bBEa").build();
    String appToken =
        SHRestAssured.given()
            .contentType("application/json")
            .body(cookieDto)
            .when()
            .post(BASE + "/ext/extoken")
            .then()
            .statusCode(200)
            .body("jwt", not(empty()))
            .extract()
            .body()
            .path("jwt")
            .toString();
    SHRestAssured.given()
        .when()
        .get(BASE + "/check_token?token=" + appToken)
        .then()
        .statusCode(200);
  }

  @Test
  public void whenExTokenWithSH_ATcallShape_thenReturn204() {
    String path = "user/customers/v1/?action=checkEmail&emailAddress=" + user.getEmail();
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getExchangeTokeByCookie(TestDataUtil.QA_SH_AT))
        .when()
        .get(path)
        .then()
        .statusCode(204);
  }

  @Test
  @Ignore
  public void whenExTokenWithSH_UTcallShape_thenReturn204() {
    String SH_UT = user.getSh_ut();
    SHCookieDto cookieDto = SHCookieDto.builder().cookie(SH_UT).build();
    String path = "/user/customers/v1/?action=forgotPassword";
    //    // for web client, add csrf token
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + getExchangeTokeByCookie(cookieDto.getCookie()))
        .header("X-csrf-Token", user.getCsrfToken())
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
  }

  private String getExchangeTokeByCookie(String cookie) {
    SHCookieDto cookieDto = SHCookieDto.builder().cookie(cookie).build();
    return tokenManagementAPI.getExchangeToken(cookieDto);
  }
}

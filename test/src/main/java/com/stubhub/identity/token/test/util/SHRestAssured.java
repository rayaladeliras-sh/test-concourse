package com.stubhub.identity.token.test.util;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

public class SHRestAssured {

  public static RequestSpecification given() {
    RequestSpecification specification =
        new RequestSpecBuilder()
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .setRelaxedHTTPSValidation()
            .setBaseUri(SHEnv.getBaseURL())
            .build();

    return RestAssured.given().spec(specification);
  }
}

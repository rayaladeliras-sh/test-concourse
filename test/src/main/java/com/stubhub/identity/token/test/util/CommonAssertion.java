package com.stubhub.identity.token.test.util;

import io.restassured.http.ContentType;
import org.hamcrest.Matchers;

public class CommonAssertion {

  public static void assertShapeLoginSuccess(String email, String password) {
    ShapeAPI.login(email, password)
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("login", Matchers.not(Matchers.isEmptyOrNullString()));
  }
}

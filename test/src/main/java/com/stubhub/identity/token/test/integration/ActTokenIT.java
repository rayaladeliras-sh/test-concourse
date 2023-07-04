package com.stubhub.identity.token.test.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.test.dto.ActClaimDto;
import com.stubhub.identity.token.test.dto.ActUserTokenDto;
import com.stubhub.identity.token.test.util.SHRestAssured;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.testng.annotations.Test;

@Slf4j
public class ActTokenIT extends BasicInit {
  private final String GUEST_CLIENT_ID = "guest";
  private final String GUEST_CLIENT_SECRET = "guest";
  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void whenGivenRightArgs_thenReturn200() {
    String token = tokenManagementAPI.getAppToken(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET).getValue();

    ActUserTokenDto actUserTokenDto = new ActUserTokenDto();
    actUserTokenDto.setClientId(GUEST_CLIENT_ID);
    actUserTokenDto.setGuid(user.getGuid());
    actUserTokenDto.setEmail(user.getEmail());
    actUserTokenDto.setAct(
        ActClaimDto.builder()
            .clientId("sample_client")
            .sub(user.getEmail())
            .roles(Arrays.asList("user", "admin"))
            .build());

    OAuth2AccessToken response =
        SHRestAssured.given()
            .auth()
            .oauth2(token)
            .when()
            .body(actUserTokenDto)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .post(BASE + "/ext/act")
            .then()
            .statusCode(200)
            .extract()
            .as(OAuth2AccessToken.class);
    log.info("act api response:{}", response);
    HashMap claims = tokenManagementAPI.getClaims(response.getValue());
    assertEquals(claims.get("sub"), user.getGuid());
    assertEquals(claims.get("user_name"), user.getEmail());
    assertTrue(claims.get("scope").toString().contains("identity:tmgt:ext:act"));
    assertNotNull(claims.get("act"));

    HashMap actClaimDto = (HashMap) claims.get("act");

    assertEquals(actClaimDto.get("sub"), user.getEmail());
    assertEquals(actClaimDto.get("client_id"), "sample_client");
    assertEquals(actClaimDto.get("client_name"), "sample_client");
    assertThat((Collection<String>) actClaimDto.get("roles"), contains("user", "admin"));

    tokenManagementAPI.refreshToken(
        GUEST_CLIENT_ID, GUEST_CLIENT_SECRET, response.getRefreshToken().getValue());
  }

  @Test
  public void whenMissingRequiredFields_thenReturn400() {
    String token = tokenManagementAPI.getAppToken(GUEST_CLIENT_ID, GUEST_CLIENT_SECRET).getValue();

    ActUserTokenDto actUserTokenDto = new ActUserTokenDto();
    actUserTokenDto.setClientId(GUEST_CLIENT_ID);
    actUserTokenDto.setGuid(user.getGuid());
    actUserTokenDto.setEmail(user.getEmail());

    // missing act sub
    actUserTokenDto.setAct(
        ActClaimDto.builder()
            .clientId("sample_client")
            .roles(Arrays.asList("user", "admin"))
            .build());

    SHRestAssured.given()
        .auth()
        .oauth2(token)
        .when()
        .body(actUserTokenDto)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .post(BASE + "/ext/act")
        .then()
        .statusCode(400);

    // missing act client id
    actUserTokenDto.setAct(
        ActClaimDto.builder().sub(user.getEmail()).roles(Arrays.asList("user", "admin")).build());

    SHRestAssured.given()
        .auth()
        .oauth2(token)
        .when()
        .body(actUserTokenDto)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .post(BASE + "/ext/act")
        .then()
        .statusCode(400);
  }

  @Test
  public void whenGiveClientWithoutRequiredScope_thenReturn403() {
    String token = tokenManagementAPI.getAppToken("sample_client", "sample_client").getValue();

    ActUserTokenDto actUserTokenDto = new ActUserTokenDto();
    actUserTokenDto.setClientId(GUEST_CLIENT_ID);
    actUserTokenDto.setGuid(user.getGuid());
    actUserTokenDto.setEmail(user.getEmail());
    actUserTokenDto.setAct(
        ActClaimDto.builder()
            .clientId("sample_client")
            .sub(user.getEmail())
            .roles(Arrays.asList("user", "admin"))
            .build());

    SHRestAssured.given()
        .auth()
        .oauth2(token)
        .when()
        .body(actUserTokenDto)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .post(BASE + "/ext/act")
        .then()
        .statusCode(403);
  }
}

package com.stubhub.identity.token.test.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import com.stubhub.identity.token.test.util.SHEnv;
import com.stubhub.identity.token.test.util.SHRestAssured;
import com.stubhub.identity.token.test.util.TestDataUtil;
import com.stubhub.identity.token.test.util.TokenManagementAPI;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class BasicInit {

  protected static final String CLAIMS_ISS = "https://apc.stubhub.com/identity/oauth";
  protected static final String BASE = "/identity/oauth/v1";
  protected TestDataUtil.LoginUser user;
  protected TokenManagementAPI tokenManagementAPI;
  protected static final String CLIENT_ID = "guest";
  protected static final String CLIENT_SECRET = "guest";

  static {
    if (null == System.getProperty("BASE_URL")) {
      System.setProperty("BASE_URL", "http://localhost:8080");
    }
  }

  public BasicInit() {
    try {
      user = TestDataUtil.UserGenerator.defaultMeta().ready().create();
      tokenManagementAPI = new TokenManagementAPI(SHEnv.getBaseURL() + "/identity/oauth");
    } catch (Exception e) {
      log.error("basic init fail with error: {}", e.getLocalizedMessage(), e);
      throw e;
    }
    log.info("generate new user {}", user);
  }

  protected String getAppToken(String scope) {
    return SHRestAssured.given()
        .auth()
        .basic("guest", "guest")
        .when()
        .post(
            BASE
                + "/token?grant_type=client_credentials"
                + (StringUtils.isEmpty(scope) ? "" : "&scope=" + scope))
        .then()
        .rootPath("access_token")
        .extract()
        .body()
        .path("access_token")
        .toString();
  }

  protected Map oidcLoginFlow(String scope) {
    HashMap<String, Object> response = new HashMap();
    String session = user.getSi();
    String code = tokenManagementAPI.getAuthorizeCode(session, CLIENT_ID, scope);
    Map tokenResponse = tokenManagementAPI.exchangeCode4Token(code, CLIENT_ID, CLIENT_SECRET);

    assertThat(tokenResponse.get("access_token").toString(), not(isEmptyString()));
    assertThat(tokenResponse.get("refresh_token").toString(), not(isEmptyString()));

    Map<String, Object> expectedValueMap = new HashMap<>();
    expectedValueMap.put("client_id", CLIENT_ID);
    expectedValueMap.put("user_name", user.getEmail().toLowerCase());
    tokenManagementAPI.checkToken(tokenResponse.get("access_token").toString(), expectedValueMap);

    Map userInfo = tokenManagementAPI.getUserInfo(tokenResponse.get("access_token").toString());
    response.put("token", tokenResponse);
    response.put("user", userInfo);
    return response;
  }
}

package com.stubhub.identity.token.service.token;

public class OAuthConstants {

  public static final String OIDC_CLAIM_KEY = "oidc-claims";

  // Claims Key
  public static final String CLAIM_SUB = "sub";

  public static final String CLAIM_USER_ID = "user_id";

  public static final String CLAIM_USER_GUID = "user_guid";

  public static final String CLAIM_USER_NAME = "user_name";

  public static final String CLAIM_ISSUED_AT = "iat";

  public static final String CLAIM_AUTH_TIME = "auth_time";

  public static final String CLAIM_ISSUE_URL = "iss";

  public static final String CLAIM_AUDIENCE = "aud";

  public static final String CLAIM_CLIENT_ID = "client_id";

  public static final String CLAIM_ACT = "act";

  public static final String ID_TOKEN = "id_token";

  public static final String SCOPE_OPENID = "openid";

  public static final String CLAIM_AZP = "azp";

  public static final String CLAIM_AT_HASH = "at_hash";

  public static final String CLAIM_NONCE = "nonce";

  // Default Claims Value
  public static final String DEFAULT_CLAIM_ISSUE_URL_VALUE =
      "https://apc.stubhub.com/identity/oauth";

  public static final String DEFAULT_CLAIM_AUDIENCE_VALUE = "api://stubhub";

  public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

  public static final String GRANT_TYPE_PASSWORD = "password";
}

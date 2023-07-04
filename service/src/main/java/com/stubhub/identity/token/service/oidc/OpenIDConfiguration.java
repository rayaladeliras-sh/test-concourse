package com.stubhub.identity.token.service.oidc;

import static com.stubhub.identity.token.service.token.OAuthConstants.DEFAULT_CLAIM_ISSUE_URL_VALUE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stubhub.identity.token.service.config.ApiVersion;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class OpenIDConfiguration {

  @JsonIgnore
  @Value("${token.management.base.url}")
  private String endpoint;

  private String baseUrl = endpoint + "/identity/oauth/";

  private String issuer = DEFAULT_CLAIM_ISSUE_URL_VALUE;
  private String authorization_endpoint = baseUrl + ApiVersion.CURRENT + "/authorize";
  private String token_endpoint = baseUrl + ApiVersion.CURRENT + "/token";
  private String userinfo_endpoint = baseUrl + ApiVersion.CURRENT + "/userinfo";
  private String registration_endpoint = baseUrl + ApiVersion.CURRENT + "/clients";
  private String jwks_uri = baseUrl + ApiVersion.CURRENT + "/.well-known/jwks.json";
  private String check_token_endpoint = baseUrl + ApiVersion.CURRENT + "/check_token";

  private List<String> response_types_supported = Arrays.asList("code");
  private List<String> grant_types_supported =
      Arrays.asList("authorization_code", "refresh_token", "password", "client_credentials");
  private List<String> subject_types_supported = Arrays.asList("public");
  private List<String> id_token_signing_alg_values_supported = Arrays.asList("RS256");
  private List<String> scopes_supported = Arrays.asList("openid", "email", "profile");
  private List<String> token_endpoint_auth_methods_supported = Arrays.asList("client_secret_basic");
  // todo nonce, at_hash, c_hash, ver, first_name, last_name, "address", "update_at",
  private List<String> claims_supported =
      Arrays.asList(
          "iss", "sub", "aud", "iat", "exp", "jti", "username", "email", "groups", "client_id");
  private List<String> introspection_endpoint_auth_methods_supported =
      Arrays.asList("client_secret_jwt");
  private boolean request_parameter_supported = false;

  @PostConstruct
  public void init() {
    authorization_endpoint = baseUrl + ApiVersion.CURRENT + "/authorize";
    token_endpoint = baseUrl + ApiVersion.CURRENT + "/token";
    userinfo_endpoint = baseUrl + ApiVersion.CURRENT + "/userinfo";
    registration_endpoint = baseUrl + ApiVersion.CURRENT + "/clients";
    jwks_uri = baseUrl + ApiVersion.CURRENT + "/.well-known/jwks.json";
    check_token_endpoint = baseUrl + ApiVersion.CURRENT + "/check_token";
  }
}

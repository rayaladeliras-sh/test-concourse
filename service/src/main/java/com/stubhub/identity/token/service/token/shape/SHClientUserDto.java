package com.stubhub.identity.token.service.token.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stubhub.identity.token.service.token.act.ActClaimDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SHClientUserDto {

  @JsonProperty("client_id")
  private String clientId;

  private String sub;
  private List<String> aud;
  private List<String> scope;
  private String iss;
  private Integer exp;
  private Integer iat;
  private String jti;
  private ActClaimDto act;

  // only for user
  @JsonProperty("user_name")
  private String userName;

  @JsonProperty("session_id")
  private String sessionId;

  private List<String> authorities;
}

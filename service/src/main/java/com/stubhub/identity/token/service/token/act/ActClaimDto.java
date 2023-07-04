package com.stubhub.identity.token.service.token.act;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ActClaimDto {

  private String sub;

  @JsonProperty("client_id")
  private String clientId;

  @JsonProperty("client_name")
  private String clientName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Collection<String> roles;
}

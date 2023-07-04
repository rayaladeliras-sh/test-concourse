package com.stubhub.identity.token.service.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OAuthClientSecretDto {

  @JsonProperty(required = true)
  private String oldSecret;

  @JsonProperty(required = true)
  private String newSecret;
}

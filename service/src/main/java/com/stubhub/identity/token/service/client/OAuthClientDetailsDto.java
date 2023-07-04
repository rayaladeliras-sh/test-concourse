package com.stubhub.identity.token.service.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class OAuthClientDetailsDto implements Serializable {

  private String clientId;

  @ToString.Exclude private String clientSecret;

  private String clientName;

  private List<String> resourceIds;

  private List<String> authorizedGrantTypes;

  private List<String> scope;

  private List<String> authorities;

  private List<String> autoApprove;

  private String additionalInformation;

  private List<String> registeredRedirectUri;

  private Integer accessTokenValiditySeconds;

  private Integer refreshTokenValiditySeconds;

  @JsonProperty(defaultValue = "ACTIVE")
  private OAuthClientDetails.Status status;

  private String clientOwner;

  private String createdBy;

  private String lastUpdateBy;
}

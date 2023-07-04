package com.stubhub.identity.token.test.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OAuthClientDetailsDto implements Serializable {

  private String clientId;

  private String clientSecret;

  private List<String> resourceIds;

  private List<String> authorizedGrantTypes;

  private List<String> scope;

  private List<String> authorities;

  private List<String> autoApprove;

  private String additionalInformation;

  private List<String> registeredRedirectUri;

  private Integer accessTokenValiditySeconds;

  private Integer refreshTokenValiditySeconds;

  private String status;

  private String clientOwner;

  private String createdBy;

  private String lastUpdateBy;
}

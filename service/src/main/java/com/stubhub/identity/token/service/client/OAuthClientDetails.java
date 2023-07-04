package com.stubhub.identity.token.service.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.spanner.v1.TypeCode;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;

@Table(name = "OAUTH_CLIENT_DETAILS")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OAuthClientDetails implements ClientDetails {
  @Id
  @PrimaryKey
  @Column(name = "CLIENT_ID")
  private String clientId;

  @Column(name = "RESOURCE_IDS")
  private List<String> resourceIds;

  @Column(name = "CLIENT_SECRET")
  private String clientSecret;

  @Column(name = "CLIENT_NAME")
  private String clientName;

  @Column(name = "SCOPE")
  private List<String> scope;

  @Column(name = "AUTHORIZED_GRANT_TYPES")
  private List<String> authorizedGrantTypes;

  @Column(name = "WEB_SERVER_REDIRECT_URI")
  private List<String> registeredRedirectUri;

  @Column(name = "AUTHORITIES")
  private List<String> authorities;

  @Column(name = "ACCESS_TOKEN_VALIDITY")
  private Integer accessTokenValiditySeconds;

  @Column(name = "REFRESH_TOKEN_VALIDITY")
  private Integer refreshTokenValiditySeconds;

  @Column(name = "ADDITIONAL_INFORMATION")
  private String additionalInformation;

  @Column(name = "AUTO_APPROVE")
  private List<String> autoApprove;

  @Column(name = "CLIENT_STATUS", spannerType = TypeCode.STRING)
  @Builder.Default
  private Status status = Status.ACTIVE;

  public enum Status {
    ACTIVE,
    INACTIVE;
  }

  @Column(name = "CREATED_BY")
  private String createdBy = "identity-pcf-default";

  @Column(name = "CREATED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date createAt;

  @Column(name = "LAST_UPDATED_BY")
  private String lastUpdateBy = "identity-pcf-default";

  @Column(name = "LAST_UPDATED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date lastUpdateAt = Date.from(Instant.now());

  @Column(name = "CLIENT_OWNER", spannerType = TypeCode.STRING)
  private String clientOwner;

  @Override
  public String getClientId() {
    return this.clientId;
  }

  @Override
  public Set<String> getResourceIds() {
    return new HashSet<>(Optional.ofNullable(this.resourceIds).orElse(Collections.emptyList()));
  }

  @Override
  public boolean isSecretRequired() {
    return this.clientSecret != null;
  }

  @Override
  public String getClientSecret() {
    return this.clientSecret;
  }

  @Override
  public boolean isScoped() {
    return this.scope != null && !this.scope.isEmpty();
  }

  @Override
  public Set<String> getScope() {
    return new HashSet<>(Optional.ofNullable(this.scope).orElse(Collections.emptyList()));
  }

  @Override
  public Set<String> getAuthorizedGrantTypes() {
    return new HashSet<>(
        Optional.ofNullable(this.authorizedGrantTypes).orElse(Collections.emptyList()));
  }

  @Override
  public Set<String> getRegisteredRedirectUri() {
    return new HashSet<>(
        Optional.ofNullable(this.registeredRedirectUri).orElse(Collections.emptyList()));
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities() {
    if (authorities == null) {
      return Collections.emptyList();
    } else {
      return this.authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }
  }

  @Override
  public Integer getAccessTokenValiditySeconds() {
    return this.accessTokenValiditySeconds;
  }

  @Override
  public Integer getRefreshTokenValiditySeconds() {
    return this.refreshTokenValiditySeconds;
  }

  @Override
  public boolean isAutoApprove(String scope) {
    if (this.autoApprove == null) {
      return false;
    } else {
      Iterator var2 = this.autoApprove.iterator();

      String auto;
      do {
        if (!var2.hasNext()) {
          return false;
        }

        auto = (String) var2.next();
      } while (!auto.equals("true") && !scope.matches(auto));

      return true;
    }
  }

  @Override
  @SneakyThrows
  public Map<String, Object> getAdditionalInformation() {
    if (Strings.isNullOrEmpty(this.additionalInformation)) {
      return new HashMap<>();
    } else {
      return new ObjectMapper()
          .readValue(this.additionalInformation, new TypeReference<Map<String, Object>>() {});
    }
  }
}

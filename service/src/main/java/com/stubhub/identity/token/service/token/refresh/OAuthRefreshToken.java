package com.stubhub.identity.token.service.token.refresh;

import com.google.spanner.v1.TypeCode;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.annotation.Id;

@Table(name = "OAUTH_REFRESH_TOKEN")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRefreshToken {
  @Id
  @PrimaryKey
  @Column(name = "TOKEN_ID")
  private String tokenId;

  @Column(name = "TOKEN")
  private String token;

  @Column(name = "AUTHENTICATION")
  private String authentication;

  @Column(name = "CREATED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date createAt;

  @Column(name = "EXPIRED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date expiredAt;
}

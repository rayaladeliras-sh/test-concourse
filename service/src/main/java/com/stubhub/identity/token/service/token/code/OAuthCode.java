package com.stubhub.identity.token.service.token.code;

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

@Table(name = "OAUTH_CODE")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OAuthCode {
  @Id
  @PrimaryKey
  @Column(name = "code")
  private String code;

  @Column(name = "AUTHENTICATION")
  private String authentication;

  @Column(name = "CREATED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date createAt;
}

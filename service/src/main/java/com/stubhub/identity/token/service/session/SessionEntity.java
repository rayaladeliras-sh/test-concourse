package com.stubhub.identity.token.service.session;

import com.google.spanner.v1.TypeCode;
import java.util.Date;
import lombok.*;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.annotation.Id;

@Table(name = "SESSION")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class SessionEntity {
  @Id
  @PrimaryKey
  @Column(name = "SESSION_ID")
  private String sessionId;

  @Column(name = "SESSION")
  private String session;

  @Column(name = "CREATED_AT", spannerType = TypeCode.TIMESTAMP)
  private Date createAt;

  @Column(name = "LAST_ACCESS", spannerType = TypeCode.TIMESTAMP)
  private Date lastAccess;
}

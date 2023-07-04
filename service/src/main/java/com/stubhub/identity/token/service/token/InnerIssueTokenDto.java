package com.stubhub.identity.token.service.token;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InnerIssueTokenDto {
  private String clientId;
  private String guid;
  private String email;
}

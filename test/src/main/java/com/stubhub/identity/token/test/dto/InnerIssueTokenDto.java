package com.stubhub.identity.token.test.dto;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerIssueTokenDto {
  private String clientId;
  private String guid;
  private String email;
}

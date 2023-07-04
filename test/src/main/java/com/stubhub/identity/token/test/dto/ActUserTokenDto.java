package com.stubhub.identity.token.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ActUserTokenDto extends InnerIssueTokenDto {
  private ActClaimDto act;
}

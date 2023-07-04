package com.stubhub.identity.token.service.token.act;

import com.stubhub.identity.token.service.token.InnerIssueTokenDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ActUserTokenDto extends InnerIssueTokenDto {
  private ActClaimDto act;
}

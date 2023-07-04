package com.stubhub.identity.token.test.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class SHCookieDto {

  private String cookie;
  private String token;
}

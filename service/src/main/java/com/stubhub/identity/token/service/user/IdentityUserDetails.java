package com.stubhub.identity.token.service.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class IdentityUserDetails {

  private String email;
  private String guid;

  private Name name;
  private Phone phone;
  private String preferedLocale;
  private String shapeUserId;
  private String status;

  private boolean legacyGuest = false;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @ToString
  public static class Name {
    private String firstName;
    private String lastName;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @ToString
  public static class Phone {
    private String phone;
    private String countryCallingCode;
    private String ext;
  }
}

package com.stubhub.identity.token.service.oidc;

import com.stubhub.identity.token.service.user.IdentityUserDetails;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScopedClaim {

  private String name;
  private List<String> properties;

  public enum ScopeGroup {
    openid,
    profile,
    email,
    address,
    groups
  }

  public static final List<ScopedClaim> SCOPED_CLAIMS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ScopedClaim(ScopeGroup.openid.name(), Arrays.asList(UserClaimConstants.USERNAME)),
              new ScopedClaim(
                  ScopeGroup.profile.name(),
                  Arrays.asList(
                      UserClaimConstants.FIRST_NAME,
                      UserClaimConstants.LAST_NAME,
                      UserClaimConstants.CREATED_AT,
                      UserClaimConstants.UPDATED_AT)),
              new ScopedClaim(ScopeGroup.email.name(), Arrays.asList(UserClaimConstants.EMAIL)),
              new ScopedClaim(
                  ScopeGroup.address.name(),
                  Arrays.asList(
                      UserClaimConstants.COUNTRY_CODE,
                      UserClaimConstants.LOCATION,
                      UserClaimConstants.STATE)),
              new ScopedClaim(
                  ScopeGroup.groups.name(), Arrays.asList(UserClaimConstants.AUTHORITIES))));

  public static Optional<ScopedClaim> getGroupProperties(String group) {
    return SCOPED_CLAIMS
        .stream()
        .filter(scopedClaim -> scopedClaim.getName().equalsIgnoreCase(group))
        .findFirst();
  }

  public static Map<String, Object> buildClaims(String group, IdentityUserDetails userDetails) {
    HashMap<String, Object> claims = new HashMap<>();
    if (ScopeGroup.openid.name().equalsIgnoreCase(group)) {
      claims.put(UserClaimConstants.USERNAME, userDetails.getEmail());
      claims.put(UserClaimConstants.EMAIL, userDetails.getEmail());
      if (null != userDetails.getName()) {
        claims.put(
            UserClaimConstants.NAME,
            (null == userDetails.getName().getFirstName())
                ? ""
                : userDetails.getName().getFirstName() + "," + userDetails.getName().getLastName());
        claims.put(UserClaimConstants.GIVEN_NAME, userDetails.getName().getFirstName());
      }
    } else if (ScopeGroup.profile.name().equalsIgnoreCase(group)) {
      claims.put(UserClaimConstants.PHONE, userDetails.getPhone());
    } else if (ScopeGroup.address.name().equalsIgnoreCase(group)) {
      claims.put(UserClaimConstants.PHONE, userDetails.getPhone());
    }
    return claims;
  }
}

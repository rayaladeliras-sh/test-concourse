package com.stubhub.identity.token.service.user;

import java.util.Collection;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

public interface IdentityUserProvider {

  Optional<IdentityUserDetails> findUserByName(String username);

  Optional<IdentityUserDetails> findUserByGuid(String guid);

  Collection<GrantedAuthority> getAuthorities(IdentityUserDetails identityUserDetails);

  Authentication authenticate(String username, String password) throws AuthenticationException;

  void put2Cache(IdentityUserDetails dto);

  void removeFromCache(String userGuid);
}

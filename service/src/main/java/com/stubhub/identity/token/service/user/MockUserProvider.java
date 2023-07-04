package com.stubhub.identity.token.service.user;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "mode.test", havingValue = "true")
public class MockUserProvider implements IdentityUserProvider {

  private final IdentityUserDetails mockUser =
      IdentityUserDetails.builder()
          .shapeUserId("1")
          .email("mock@stubhub.com")
          .guid("1")
          .name(new IdentityUserDetails.Name("Mock", "Test"))
          .status("ACTIVE")
          .build();

  @Override
  public Optional<IdentityUserDetails> findUserByName(String username) {
    return Optional.of(mockUser);
  }

  @Override
  public Optional<IdentityUserDetails> findUserByGuid(String guid) {
    return Optional.of(mockUser);
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities(IdentityUserDetails identityUserDetails) {
    return Arrays.asList(new SimpleGrantedAuthority("USER"));
  }

  @Override
  public Authentication authenticate(String username, String password)
      throws AuthenticationException {
    return new UsernamePasswordAuthenticationToken(
        username, password, Arrays.asList(new SimpleGrantedAuthority("USER")));
  }

  @Override
  public void put2Cache(IdentityUserDetails dto) {}

  @Override
  public void removeFromCache(String userGuid) {}
}

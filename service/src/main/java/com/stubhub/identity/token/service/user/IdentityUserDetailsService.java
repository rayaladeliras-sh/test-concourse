package com.stubhub.identity.token.service.user;

import com.stubhub.identity.token.service.exception.ExceptionSupplier;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdentityUserDetailsService implements UserDetailsService {

  @Autowired private IdentityUserProvider identityUserProvider;

  public String getRandomPassword() {
    return randomPassword;
  }

  private String randomPassword;

  @PostConstruct
  public void init() {
    randomPassword = UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return toUserDetails(findUserByEmail(username));
  }

  public IdentityUserDetails findUserByGuid(String guid) throws UsernameNotFoundException {
    IdentityUserDetails userDetails =
        identityUserProvider
            .findUserByGuid(guid)
            .orElseThrow(
                new ExceptionSupplier<>(new UsernameNotFoundException("User is not found")));
    log.info("find user by guid result : {}", userDetails);
    return userDetails;
  }

  public IdentityUserDetails findUserByEmail(String username) throws UsernameNotFoundException {
    IdentityUserDetails userDetails =
        identityUserProvider
            .findUserByName(username)
            .orElseThrow(
                new ExceptionSupplier<>(new UsernameNotFoundException("User is not found")));

    log.info("find user by email result : {}", userDetails);
    return userDetails;
  }

  public Authentication authenticate(String username, String password) {
    return identityUserProvider.authenticate(username, password);
  }

  public void put2Cache(IdentityUserDetails userDetails) {
    identityUserProvider.put2Cache(userDetails);
  }

  public void removeUserFromCache(String guid) {
    identityUserProvider.removeFromCache(guid);
  }

  private UserDetails toUserDetails(IdentityUserDetails identityUserDetails) {
    OAuthUserDetails oAuthUserDetails =
        OAuthUserDetails.builder()
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .username(identityUserDetails.getEmail())
            // we don't have user authenticate here, just set a false password for internal use
            // we'll check client id and secret or jwt, so i think it's safe
            .password(randomPassword)
            .isEnabled("ACTIVE".equalsIgnoreCase(identityUserDetails.getStatus()))
            .build();
    if (identityUserDetails.isLegacyGuest()) {
      oAuthUserDetails.setAuthorities(Arrays.asList(new SimpleGrantedAuthority("GUEST")));
    }
    return oAuthUserDetails;
  }
}

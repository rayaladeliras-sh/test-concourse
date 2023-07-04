package com.stubhub.identity.token.service.user;

import com.stubhub.identity.token.service.exception.ExtendedErrorException;
import com.stubhub.identity.token.service.token.IdentityTokenService;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@ConditionalOnProperty(value = "mode.test", havingValue = "false", matchIfMissing = true)
public class CloudUserProvider implements IdentityUserProvider {
  @Value("${remote.api.customer.identity.v1.endpoint}")
  private String customerIdentityEndpoint;

  @Value("${security.oauth2.client.clientId}")
  private String clientId;

  @Value("${security.oauth2.client.clientSecret}")
  private String clientSecret;

  @Autowired private RestTemplate restTemplate;

  @Autowired @Lazy private IdentityTokenService identityTokenService;

  @Autowired private CacheManager cacheManager;

  @Override
  public Optional<IdentityUserDetails> findUserByName(String username) {
    return findUserByEmail(username);
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities(IdentityUserDetails identityUserDetails) {
    return null;
  }

  @Override
  public Authentication authenticate(String username, String password)
      throws AuthenticationException {
    return null;
  }

  public CaffeineCache userCache() {
    return (CaffeineCache) cacheManager.getCache("users");
  }

  public void put2Cache(IdentityUserDetails dto) {
    userCache().putIfAbsent(dto.getGuid(), dto);
  }

  public void removeFromCache(String userGuid) {
    userCache().evict(userGuid);
  }

  public Optional<IdentityUserDetails> findUserByGuid(String userGuid)
      throws ExtendedErrorException {
    Optional<Object> identityUserDetailsDto = findUserByGuidFromCache(userGuid);
    if (identityUserDetailsDto.isPresent()) {
      log.debug("Found user {} from cache", userGuid);
      return Optional.of((IdentityUserDetails) identityUserDetailsDto.get());
    } else {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("guid", userGuid);
      return Optional.of(findUser(params));
    }
  }

  public Optional<IdentityUserDetails> findUserByEmail(String email) throws ExtendedErrorException {
    Optional<Object> identityUserDetailsDto = findUserByEmailFromCache(email);
    if (identityUserDetailsDto.isPresent()) {
      log.debug("Found user {} from cache", email);
      return Optional.of((IdentityUserDetails) identityUserDetailsDto.get());
    } else {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("email", email);
      return Optional.of(findUser(params));
    }
  }

  private Optional<Object> findUserByGuidFromCache(String guid) {
    if (null != userCache().get(guid)) {
      return Optional.of(userCache().get(guid).get());
    } else {
      return Optional.empty();
    }
  }

  private Optional<Object> findUserByEmailFromCache(String email) {
    try {
      return userCache()
          .getNativeCache()
          .asMap()
          .values()
          .stream()
          .filter(item -> ((IdentityUserDetails) item).getEmail().equals(email))
          .findFirst();
    } catch (Exception e) {
      log.warn("Find user {} by cache with error: {}", email, e.getLocalizedMessage());
    }
    return Optional.empty();
  }

  private IdentityUserDetails findUser(MultiValueMap<String, String> params)
      throws ExtendedErrorException {
    log.info(
        "method=findUser, uri={}, httpMethod={}, reqParams={}, message=\"Find user from remote service\"",
        customerIdentityEndpoint,
        HttpMethod.GET,
        params);
    StopWatch sw = new StopWatch();
    sw.start();
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(customerIdentityEndpoint).queryParams(params);
    String url = uriBuilder.toUriString();
    MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
    header.add("Accept", MediaType.APPLICATION_JSON.toString());
    header.add(
        "Authorization", "Bearer " + identityTokenService.requestAppToken(clientId, clientSecret));
    HttpEntity<String> req = new HttpEntity<>(header);
    try {
      ResponseEntity<IdentityUserDetails> res =
          restTemplate.exchange(url, HttpMethod.GET, req, IdentityUserDetails.class);
      sw.stop();
      log.info(
          "api=tms_remoteapi_finduser, statusCode={}, duration={} ms",
          res.getStatusCode(),
          sw.getTotalTimeMillis());
      log.info("method=findUser find user result {}", res.getBody());
      put2Cache(res.getBody());
      return res.getBody();
    } catch (RestClientResponseException e) {
      log.error(
          "method=findUser, errMsg=\"remote call {} fail with {} {}\"",
          url,
          e.getStatusText(),
          e.getResponseBodyAsString());
      throw e;
    }
  }
}

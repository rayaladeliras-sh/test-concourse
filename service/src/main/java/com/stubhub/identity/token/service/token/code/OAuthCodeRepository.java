package com.stubhub.identity.token.service.token.code;

import java.util.Optional;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;

public interface OAuthCodeRepository extends SpannerRepository<OAuthCode, String> {
  Optional<OAuthCode> findTopByCode(String code);
}

package com.stubhub.identity.token.service.client;

import com.stubhub.identity.token.service.client.OAuthClientDetails.Status;
import java.util.Optional;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthClientDetailsRepository
    extends SpannerRepository<OAuthClientDetails, String> {
  Optional<OAuthClientDetails> findByClientIdAndStatus(String clientId, Status status);

  Optional<OAuthClientDetails> findByClientNameAndStatus(String clientName, Status status);
}

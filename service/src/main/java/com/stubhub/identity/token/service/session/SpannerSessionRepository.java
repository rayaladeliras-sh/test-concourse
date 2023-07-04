package com.stubhub.identity.token.service.session;

import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpannerSessionRepository extends SpannerRepository<SessionEntity, String> {}

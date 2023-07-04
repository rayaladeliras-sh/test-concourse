package com.stubhub.identity.token.service.token.refresh;

import java.util.List;
import java.util.Optional;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.cloud.gcp.data.spanner.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthRefreshTokenRepository extends SpannerRepository<OAuthRefreshToken, String> {
  @Query(
      value =
          "delete from OAUTH_REFRESH_TOKEN where CURRENT_TIMESTAMP > OAUTH_REFRESH_TOKEN.EXPIRED_AT",
      dmlStatement = true)
  void clearExpiredRefreshToken();

  // todo performance tuning for the query
  @Query(
      value =
          "select * from OAUTH_REFRESH_TOKEN where CURRENT_TIMESTAMP > OAUTH_REFRESH_TOKEN.EXPIRED_AT limit 100")
  Optional<List<OAuthRefreshToken>> getExpiredRefreshToken();
}

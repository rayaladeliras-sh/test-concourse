package com.stubhub.identity.token.service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {

  @Value("${spring.cache.caffeine.user.max-size}")
  private long userMaxSize;

  @Value("${spring.cache.caffeine.user.expire-time}")
  private long userExpireTime;

  @Value("${spring.cache.caffeine.client.max-size}")
  private long clientMaxSize;

  @Value("${spring.cache.caffeine.client.expire-time}")
  private long clientExpireTime;

  @Value("${spring.cache.caffeine.session.legacy.max-size}")
  private long legacySessionSize;

  @Value("${spring.cache.caffeine.session.legacy.expire-time}")
  private long legacySessionExpireTime;

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(
        Arrays.asList(
            buildCache("users", userMaxSize, userExpireTime),
            buildCache("clients", clientMaxSize, clientExpireTime),
            buildCache("legacySession", legacySessionSize, legacySessionExpireTime)));
    return manager;
  }

  private CaffeineCache buildCache(String name, long maxSize, long expireTime) {
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .expireAfterWrite(expireTime, TimeUnit.SECONDS)
            .maximumSize(maxSize)
            .ticker(ticker())
            .build());
  }

  @Bean
  public Ticker ticker() {
    return Ticker.systemTicker();
  }
}

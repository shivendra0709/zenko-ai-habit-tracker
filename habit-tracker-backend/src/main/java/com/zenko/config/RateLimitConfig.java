package com.zenko.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(20) // 20 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        // Create named rate limiters for AI endpoints
        registry.rateLimiter("aiEndpoints", RateLimiterConfig.custom()
            .limitForPeriod(10) // 10 requests
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(10))
            .build());

        registry.rateLimiter("geminiApi", RateLimiterConfig.custom()
            .limitForPeriod(30) // 30 requests
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(10))
            .build());

        return registry;
    }
}

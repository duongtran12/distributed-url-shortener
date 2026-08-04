package com.duong.url_shortener.ratelimit;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		boolean failOpen,
		@NotNull Duration window,
		@Min(1) int authRequests,
		@Min(1) int redirectRequests,
		@Min(1) int apiRequests) {
}

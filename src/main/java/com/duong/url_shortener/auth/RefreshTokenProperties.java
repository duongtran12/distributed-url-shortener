package com.duong.url_shortener.auth;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
		@NotNull Duration expiration,
		boolean secureCookie,
		@Min(1) int maxActiveSessions) {
}

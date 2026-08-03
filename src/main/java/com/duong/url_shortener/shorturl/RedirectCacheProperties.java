package com.duong.url_shortener.shorturl;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.redirect-cache")
public record RedirectCacheProperties(
		boolean enabled,
		@NotNull Duration ttl) {
}

package com.duong.url_shortener.shorturl;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.audit-retention")
public record AuditRetentionProperties(
		boolean enabled,
		@NotNull Duration retention,
		@Min(1) @Max(10_000) int batchSize) {
}

package com.duong.url_shortener.shorturl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.short-url")
public record ShortUrlProperties(
		@Min(4) @Max(32) int codeLength,
		@Min(1) @Max(20) int maxGenerationAttempts,
		@NotBlank String baseUrl) {
}

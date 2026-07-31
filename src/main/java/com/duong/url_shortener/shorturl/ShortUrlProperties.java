package com.duong.url_shortener.shorturl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.short-url")
public record ShortUrlProperties(
		@Min(4) @Max(32) int codeLength) {
}

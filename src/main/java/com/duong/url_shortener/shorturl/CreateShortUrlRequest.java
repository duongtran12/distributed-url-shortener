package com.duong.url_shortener.shorturl;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
		@NotBlank @Size(max = 2048) String originalUrl,
		@Size(max = 120) String title,
		@Size(max = 32) String customAlias,
		Instant expiresAt) {
}

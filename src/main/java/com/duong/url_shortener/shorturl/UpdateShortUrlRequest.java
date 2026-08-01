package com.duong.url_shortener.shorturl;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateShortUrlRequest(
		@NotBlank @Size(max = 2048) String originalUrl,
		Instant expiresAt) {
}

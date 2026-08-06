package com.duong.url_shortener.shorturl;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateShortUrlRequest(
		@NotBlank @Size(max = 2048) String originalUrl,
		@Size(max = 120) String title,
		@Size(max = 32) @Pattern(regexp = "^$|[A-Za-z0-9][A-Za-z0-9_-]*$") String tag,
		Instant expiresAt) {
}

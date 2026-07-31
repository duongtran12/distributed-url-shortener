package com.duong.url_shortener.shorturl;

import java.time.Instant;

public record ShortUrlResponse(
		Long id,
		String shortCode,
		String shortUrl,
		String originalUrl,
		ShortUrlStatus status,
		boolean customAlias,
		Instant expiresAt,
		Instant createdAt) {

	static ShortUrlResponse from(ShortUrl shortUrl, String baseUrl) {
		String normalizedBaseUrl = baseUrl.endsWith("/")
				? baseUrl.substring(0, baseUrl.length() - 1)
				: baseUrl;

		return new ShortUrlResponse(
				shortUrl.getId(),
				shortUrl.getShortCode(),
				normalizedBaseUrl + "/" + shortUrl.getShortCode(),
				shortUrl.getOriginalUrl(),
				shortUrl.getStatus(),
				shortUrl.isCustomAlias(),
				shortUrl.getExpiresAt(),
				shortUrl.getCreatedAt());
	}
}

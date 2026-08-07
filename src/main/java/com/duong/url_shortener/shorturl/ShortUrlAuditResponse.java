package com.duong.url_shortener.shorturl;

import java.time.Instant;

public record ShortUrlAuditResponse(
		Long id,
		Long shortUrlId,
		String shortCode,
		ShortUrlAuditAction action,
		String details,
		Instant createdAt) {

	static ShortUrlAuditResponse from(ShortUrlAuditEvent event) {
		return new ShortUrlAuditResponse(
				event.getId(),
				event.getShortUrl() == null ? null : event.getShortUrl().getId(),
				event.getShortCode(),
				event.getAction(),
				event.getDetails(),
				event.getCreatedAt());
	}
}

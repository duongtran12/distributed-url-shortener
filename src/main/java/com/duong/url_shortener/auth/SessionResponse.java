package com.duong.url_shortener.auth;

import java.time.Instant;

public record SessionResponse(
		Long id,
		String userAgent,
		Instant createdAt,
		Instant lastUsedAt,
		Instant expiresAt,
		boolean current) {

	static SessionResponse from(RefreshToken token, String currentTokenHash) {
		return new SessionResponse(
				token.getId(),
				token.getUserAgent(),
				token.getCreatedAt(),
				token.getLastUsedAt(),
				token.getExpiresAt(),
				token.getTokenHash().equals(currentTokenHash));
	}
}

package com.duong.url_shortener.ratelimit;

public record RateLimitDecision(
		boolean allowed,
		long limit,
		long remaining,
		long retryAfterSeconds) {

	public static RateLimitDecision allowedWithoutLimit() {
		return new RateLimitDecision(true, 0, 0, 0);
	}
}

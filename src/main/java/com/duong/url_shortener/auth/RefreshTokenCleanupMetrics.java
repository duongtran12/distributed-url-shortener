package com.duong.url_shortener.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupMetrics {

	private final Counter deletedTokens;

	public RefreshTokenCleanupMetrics(MeterRegistry meterRegistry) {
		deletedTokens = Counter.builder("shortener.auth.refresh_tokens.deleted")
				.description("Number of expired or revoked refresh tokens deleted")
				.register(meterRegistry);
	}

	void recordDeleted(int count) {
		deletedTokens.increment(count);
	}
}

package com.duong.url_shortener.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetCleanupMetrics {
	private final Counter deletedTokens;

	public PasswordResetCleanupMetrics(MeterRegistry meterRegistry) {
		deletedTokens = Counter.builder("shortener.auth.password_reset_tokens.deleted")
				.description("Number of stale password reset tokens deleted")
				.register(meterRegistry);
	}

	void recordDeleted(int count) {
		deletedTokens.increment(count);
	}
}

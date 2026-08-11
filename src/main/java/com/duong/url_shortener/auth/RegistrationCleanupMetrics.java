package com.duong.url_shortener.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RegistrationCleanupMetrics {

	private final Counter deletedRegistrations;

	public RegistrationCleanupMetrics(MeterRegistry meterRegistry) {
		deletedRegistrations = Counter.builder("shortener.auth.registrations.abandoned.deleted")
				.description("Number of abandoned unverified registrations deleted")
				.register(meterRegistry);
	}

	void recordDeleted(int count) {
		deletedRegistrations.increment(count);
	}
}

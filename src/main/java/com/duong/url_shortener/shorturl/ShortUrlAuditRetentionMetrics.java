package com.duong.url_shortener.shorturl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlAuditRetentionMetrics {

	private final Counter deletedEvents;

	public ShortUrlAuditRetentionMetrics(MeterRegistry meterRegistry) {
		deletedEvents = Counter.builder("shortener.audit.retention.deleted")
				.description("Number of expired short URL audit events deleted")
				.register(meterRegistry);
	}

	void recordDeleted(int count) {
		deletedEvents.increment(count);
	}
}

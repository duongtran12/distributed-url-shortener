package com.duong.url_shortener.click;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ClickEventRetentionMetrics {

	private final Counter deletedEvents;

	public ClickEventRetentionMetrics(MeterRegistry meterRegistry) {
		deletedEvents = Counter.builder("shortener.click.retention.deleted")
				.description("Number of expired raw click events deleted")
				.register(meterRegistry);
	}

	void recordDeleted(int count) {
		deletedEvents.increment(count);
	}
}

package com.duong.url_shortener.shorturl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RedirectMetrics {

	private final MeterRegistry meterRegistry;

	public RedirectMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordResolution(String source) {
		Counter.builder("shortener.redirect.resolutions")
				.description("Successfully resolved short URL redirects")
				.tag("source", source)
				.register(meterRegistry)
				.increment();
	}

	public void recordFailure(String reason) {
		Counter.builder("shortener.redirect.failures")
				.description("Failed short URL redirect resolutions")
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}
}

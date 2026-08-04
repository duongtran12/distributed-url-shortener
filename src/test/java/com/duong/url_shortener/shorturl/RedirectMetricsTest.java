package com.duong.url_shortener.shorturl;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RedirectMetricsTest {

	@Test
	void shouldRecordRedirectOutcomesWithBoundedTags() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		RedirectMetrics metrics = new RedirectMetrics(registry);

		metrics.recordResolution("cache");
		metrics.recordResolution("database");
		metrics.recordFailure("not_found");
		metrics.recordFailure("unavailable");

		assertThat(registry.get("shortener.redirect.resolutions")
				.tag("source", "cache").counter().count()).isEqualTo(1);
		assertThat(registry.get("shortener.redirect.resolutions")
				.tag("source", "database").counter().count()).isEqualTo(1);
		assertThat(registry.get("shortener.redirect.failures")
				.tag("reason", "not_found").counter().count()).isEqualTo(1);
		assertThat(registry.get("shortener.redirect.failures")
				.tag("reason", "unavailable").counter().count()).isEqualTo(1);
	}
}

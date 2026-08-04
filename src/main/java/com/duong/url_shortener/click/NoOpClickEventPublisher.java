package com.duong.url_shortener.click;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "app.click-tracking",
		name = "enabled",
		havingValue = "false")
public class NoOpClickEventPublisher implements ClickEventPublisher {

	@Override
	public void publish(
			String shortCode,
			Instant clickedAt,
			String userAgent,
			String referrer,
			String visitorHash) {
		// Click tracking is intentionally disabled for this environment.
	}
}

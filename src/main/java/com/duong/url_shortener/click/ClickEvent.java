package com.duong.url_shortener.click;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
		UUID eventId,
		String shortCode,
		Instant clickedAt,
		String userAgent,
		String referrer) {

	public ClickEvent(UUID eventId, String shortCode, Instant clickedAt) {
		this(eventId, shortCode, clickedAt, null, null);
	}

	public static ClickEvent create(String shortCode, Instant clickedAt) {
		return create(shortCode, clickedAt, null, null);
	}

	public static ClickEvent create(
			String shortCode,
			Instant clickedAt,
			String userAgent,
			String referrer) {
		return new ClickEvent(UUID.randomUUID(), shortCode, clickedAt, userAgent, referrer);
	}
}

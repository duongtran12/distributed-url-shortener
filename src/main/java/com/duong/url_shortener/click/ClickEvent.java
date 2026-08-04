package com.duong.url_shortener.click;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
		UUID eventId,
		String shortCode,
		Instant clickedAt,
		String userAgent,
		String referrer,
		String visitorHash) {

	public ClickEvent(UUID eventId, String shortCode, Instant clickedAt) {
		this(eventId, shortCode, clickedAt, null, null, null);
	}

	public static ClickEvent create(String shortCode, Instant clickedAt) {
		return create(shortCode, clickedAt, null, null, null);
	}

	public static ClickEvent create(
			String shortCode,
			Instant clickedAt,
			String userAgent,
			String referrer) {
		return create(shortCode, clickedAt, userAgent, referrer, null);
	}

	public static ClickEvent create(
			String shortCode,
			Instant clickedAt,
			String userAgent,
			String referrer,
			String visitorHash) {
		return new ClickEvent(
				UUID.randomUUID(), shortCode, clickedAt, userAgent, referrer, visitorHash);
	}
}

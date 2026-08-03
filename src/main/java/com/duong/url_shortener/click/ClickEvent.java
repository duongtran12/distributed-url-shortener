package com.duong.url_shortener.click;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
		UUID eventId,
		String shortCode,
		Instant clickedAt) {

	public static ClickEvent create(String shortCode, Instant clickedAt) {
		return new ClickEvent(UUID.randomUUID(), shortCode, clickedAt);
	}
}

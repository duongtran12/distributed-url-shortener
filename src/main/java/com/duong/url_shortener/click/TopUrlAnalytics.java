package com.duong.url_shortener.click;

public record TopUrlAnalytics(
		Long shortUrlId,
		String shortCode,
		String originalUrl,
		long clicks,
		long uniqueVisitors) {
}

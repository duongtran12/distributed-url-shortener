package com.duong.url_shortener.click;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsOverviewResponse(
		long totalUrls,
		long activeUrls,
		long lifetimeClicks,
		long periodClicks,
		long periodUniqueVisitors,
		LocalDate from,
		LocalDate to,
		List<DailyClickCount> dailyClicks,
		List<TopUrlAnalytics> topUrls) {
}

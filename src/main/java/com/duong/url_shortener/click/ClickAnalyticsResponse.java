package com.duong.url_shortener.click;

import java.time.LocalDate;
import java.util.List;

public record ClickAnalyticsResponse(
		Long shortUrlId,
		String shortCode,
		long lifetimeClicks,
		long periodClicks,
		LocalDate from,
		LocalDate to,
		List<DailyClickCount> dailyClicks,
		List<CategoryClickCount> browsers,
		List<CategoryClickCount> operatingSystems,
		List<CategoryClickCount> devices,
		List<CategoryClickCount> referrers) {
}

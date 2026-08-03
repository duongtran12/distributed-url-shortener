package com.duong.url_shortener.click;

import java.time.LocalDate;

public record DailyClickCount(
		LocalDate date,
		long clicks) {
}

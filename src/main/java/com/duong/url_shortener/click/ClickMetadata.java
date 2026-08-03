package com.duong.url_shortener.click;

public record ClickMetadata(
		String userAgent,
		String referrer,
		String browser,
		String operatingSystem,
		DeviceType deviceType) {
}

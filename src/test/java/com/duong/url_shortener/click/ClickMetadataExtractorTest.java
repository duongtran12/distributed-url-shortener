package com.duong.url_shortener.click;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ClickMetadataExtractorTest {

	private final ClickMetadataExtractor extractor = new ClickMetadataExtractor();

	@Test
	void shouldDetectChromeOnWindowsDesktopAndNormalizeReferrer() {
		ClickEvent event = ClickEvent.create(
				"abc1234",
				Instant.now(),
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
						+ "Chrome/126.0.0.0 Safari/537.36",
				"https://WWW.Google.COM/search?q=sensitive");

		ClickMetadata metadata = extractor.extract(event);

		assertEquals("Chrome", metadata.browser());
		assertEquals("Windows", metadata.operatingSystem());
		assertEquals(DeviceType.DESKTOP, metadata.deviceType());
		assertEquals("www.google.com", metadata.referrer());
	}

	@Test
	void shouldDetectMobileSafariAndDirectTraffic() {
		ClickEvent event = ClickEvent.create(
				"abc1234",
				Instant.now(),
				"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
						+ "AppleWebKit/605.1.15 Mobile/15E148 Version/17.0 Safari/604.1",
				null);

		ClickMetadata metadata = extractor.extract(event);

		assertEquals("Safari", metadata.browser());
		assertEquals("iOS", metadata.operatingSystem());
		assertEquals(DeviceType.MOBILE, metadata.deviceType());
		assertEquals("direct", metadata.referrer());
	}

	@Test
	void shouldDetectBotsAndHandleMissingUserAgent() {
		ClickMetadata bot = extractor.extract(ClickEvent.create(
				"abc1234", Instant.now(), "Googlebot/2.1", "not a URI"));
		ClickMetadata missing = extractor.extract(ClickEvent.create(
				"abc1234", Instant.now(), null, null));

		assertEquals(DeviceType.BOT, bot.deviceType());
		assertEquals("unknown", bot.referrer());
		assertNull(missing.userAgent());
		assertEquals(DeviceType.UNKNOWN, missing.deviceType());
	}
}

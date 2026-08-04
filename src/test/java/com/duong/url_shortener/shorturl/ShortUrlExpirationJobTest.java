package com.duong.url_shortener.shorturl;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortUrlExpirationJobTest {

	@Mock
	private ShortUrlRepository shortUrlRepository;

	@Test
	void shouldDisableExpiredActiveUrlsAtCurrentTime() {
		Instant now = Instant.parse("2026-08-04T08:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		ShortUrlExpirationJob job = new ShortUrlExpirationJob(shortUrlRepository, clock);

		job.disableExpiredUrls();

		verify(shortUrlRepository).disableExpiredUrls(
				now,
				ShortUrlStatus.ACTIVE,
				ShortUrlStatus.DISABLED);
	}
}

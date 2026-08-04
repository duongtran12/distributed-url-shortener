package com.duong.url_shortener.shorturl;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
		prefix = "app.expiration-cleanup",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class ShortUrlExpirationJob {

	private static final Logger log = LoggerFactory.getLogger(ShortUrlExpirationJob.class);

	private final ShortUrlRepository shortUrlRepository;
	private final Clock clock;

	public ShortUrlExpirationJob(ShortUrlRepository shortUrlRepository, Clock clock) {
		this.shortUrlRepository = shortUrlRepository;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.expiration-cleanup.initial-delay:PT1M}",
			fixedDelayString = "${app.expiration-cleanup.interval:PT5M}")
	@Transactional
	public void disableExpiredUrls() {
		Instant now = clock.instant();
		int updated = shortUrlRepository.disableExpiredUrls(
				now,
				ShortUrlStatus.ACTIVE,
				ShortUrlStatus.DISABLED);

		if (updated > 0) {
			log.info("Disabled {} expired short URLs", updated);
		}
	}
}

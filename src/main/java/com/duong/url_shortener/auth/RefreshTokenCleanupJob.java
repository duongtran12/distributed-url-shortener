package com.duong.url_shortener.auth;

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
		prefix = "app.refresh-token-cleanup",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class RefreshTokenCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

	private final RefreshTokenRepository repository;
	private final RefreshTokenCleanupProperties properties;
	private final RefreshTokenCleanupMetrics metrics;
	private final Clock clock;

	public RefreshTokenCleanupJob(
			RefreshTokenRepository repository,
			RefreshTokenCleanupProperties properties,
			RefreshTokenCleanupMetrics metrics,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.metrics = metrics;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.refresh-token-cleanup.initial-delay:PT5M}",
			fixedDelayString = "${app.refresh-token-cleanup.interval:PT24H}")
	@Transactional
	public void deleteStaleTokens() {
		Instant cutoff = clock.instant().minus(properties.retention());
		int totalDeleted = 0;
		int deleted;
		do {
			deleted = repository.deleteStaleBatch(cutoff, properties.batchSize());
			totalDeleted += deleted;
		} while (deleted == properties.batchSize());

		if (totalDeleted > 0) {
			metrics.recordDeleted(totalDeleted);
			log.info("Deleted {} refresh tokens expired or revoked before {}", totalDeleted, cutoff);
		}
	}
}

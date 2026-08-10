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
		prefix = "app.password-reset-cleanup",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class PasswordResetCleanupJob {
	private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanupJob.class);

	private final PasswordResetTokenRepository repository;
	private final PasswordResetCleanupProperties properties;
	private final PasswordResetCleanupMetrics metrics;
	private final Clock clock;

	public PasswordResetCleanupJob(
			PasswordResetTokenRepository repository,
			PasswordResetCleanupProperties properties,
			PasswordResetCleanupMetrics metrics,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.metrics = metrics;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.password-reset-cleanup.initial-delay:PT5M}",
			fixedDelayString = "${app.password-reset-cleanup.interval:PT24H}")
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
			log.info("Deleted {} password reset tokens stale before {}", totalDeleted, cutoff);
		}
	}
}

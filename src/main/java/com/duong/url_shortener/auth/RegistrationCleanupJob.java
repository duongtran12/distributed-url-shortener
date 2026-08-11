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
		prefix = "app.registration-cleanup",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class RegistrationCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(RegistrationCleanupJob.class);

	private final RegistrationCleanupRepository repository;
	private final RegistrationCleanupProperties properties;
	private final RegistrationCleanupMetrics metrics;
	private final Clock clock;

	public RegistrationCleanupJob(
			RegistrationCleanupRepository repository,
			RegistrationCleanupProperties properties,
			RegistrationCleanupMetrics metrics,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.metrics = metrics;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.registration-cleanup.initial-delay:PT5M}",
			fixedDelayString = "${app.registration-cleanup.interval:PT24H}")
	@Transactional
	public void deleteAbandonedRegistrations() {
		Instant cutoff = clock.instant().minus(properties.retention());
		int totalDeleted = 0;
		int deleted;
		do {
			deleted = repository.deleteOldestBatch(cutoff, properties.batchSize());
			totalDeleted += deleted;
		} while (deleted == properties.batchSize());

		if (totalDeleted > 0) {
			metrics.recordDeleted(totalDeleted);
			log.info("Deleted {} unverified registrations created before {}", totalDeleted, cutoff);
		}
	}
}

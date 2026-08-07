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
		prefix = "app.audit-retention",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class ShortUrlAuditRetentionJob {

	private static final Logger log = LoggerFactory.getLogger(ShortUrlAuditRetentionJob.class);

	private final ShortUrlAuditRepository auditRepository;
	private final AuditRetentionProperties properties;
	private final ShortUrlAuditRetentionMetrics metrics;
	private final Clock clock;

	public ShortUrlAuditRetentionJob(
			ShortUrlAuditRepository auditRepository,
			AuditRetentionProperties properties,
			ShortUrlAuditRetentionMetrics metrics,
			Clock clock) {
		this.auditRepository = auditRepository;
		this.properties = properties;
		this.metrics = metrics;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.audit-retention.initial-delay:PT5M}",
			fixedDelayString = "${app.audit-retention.interval:PT24H}")
	@Transactional
	public void deleteExpiredEvents() {
		Instant cutoff = clock.instant().minus(properties.retention());
		int totalDeleted = 0;
		int deleted;
		do {
			deleted = auditRepository.deleteOldestBatch(cutoff, properties.batchSize());
			totalDeleted += deleted;
		} while (deleted == properties.batchSize());

		if (totalDeleted > 0) {
			metrics.recordDeleted(totalDeleted);
			log.info("Deleted {} audit events older than {}", totalDeleted, cutoff);
		}
	}
}

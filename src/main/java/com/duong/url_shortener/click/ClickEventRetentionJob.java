package com.duong.url_shortener.click;

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
		prefix = "app.click-event-retention",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class ClickEventRetentionJob {

	private static final Logger log = LoggerFactory.getLogger(ClickEventRetentionJob.class);

	private final ClickEventRetentionRepository repository;
	private final ClickEventRetentionProperties properties;
	private final ClickEventRetentionMetrics metrics;
	private final Clock clock;

	public ClickEventRetentionJob(
			ClickEventRetentionRepository repository,
			ClickEventRetentionProperties properties,
			ClickEventRetentionMetrics metrics,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.metrics = metrics;
		this.clock = clock;
	}

	@Scheduled(
			initialDelayString = "${app.click-event-retention.initial-delay:PT5M}",
			fixedDelayString = "${app.click-event-retention.interval:PT24H}")
	@Transactional
	public void deleteExpiredEvents() {
		Instant cutoff = clock.instant().minus(properties.retention());
		int totalDeleted = 0;
		int deleted;
		do {
			deleted = repository.deleteOldestBatch(cutoff, properties.batchSize());
			totalDeleted += deleted;
		} while (deleted == properties.batchSize());

		if (totalDeleted > 0) {
			metrics.recordDeleted(totalDeleted);
			log.info("Deleted {} raw click events older than {}", totalDeleted, cutoff);
		}
	}
}

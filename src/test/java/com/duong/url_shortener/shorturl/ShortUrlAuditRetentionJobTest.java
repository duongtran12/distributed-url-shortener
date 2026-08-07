package com.duong.url_shortener.shorturl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortUrlAuditRetentionJobTest {

	@Mock
	private ShortUrlAuditRepository auditRepository;

	@Mock
	private ShortUrlAuditRetentionMetrics metrics;

	@Test
	void shouldDeleteExpiredEventsInBatchesAndRecordMetric() {
		Instant now = Instant.parse("2026-08-07T04:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(365));
		AuditRetentionProperties properties =
				new AuditRetentionProperties(true, Duration.ofDays(365), 500);
		when(auditRepository.deleteOldestBatch(cutoff, 500)).thenReturn(500, 27);
		ShortUrlAuditRetentionJob job = new ShortUrlAuditRetentionJob(
				auditRepository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteExpiredEvents();

		verify(auditRepository, times(2)).deleteOldestBatch(cutoff, 500);
		verify(metrics).recordDeleted(527);
	}

	@Test
	void shouldNotRecordMetricWhenNothingIsExpired() {
		Instant now = Instant.parse("2026-08-07T04:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(30));
		AuditRetentionProperties properties =
				new AuditRetentionProperties(true, Duration.ofDays(30), 100);
		when(auditRepository.deleteOldestBatch(cutoff, 100)).thenReturn(0);
		ShortUrlAuditRetentionJob job = new ShortUrlAuditRetentionJob(
				auditRepository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteExpiredEvents();

		verifyNoInteractions(metrics);
	}
}

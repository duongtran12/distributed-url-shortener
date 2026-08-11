package com.duong.url_shortener.click;

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
class ClickEventRetentionJobTest {

	@Mock
	private ClickEventRetentionRepository repository;

	@Mock
	private ClickEventRetentionMetrics metrics;

	@Test
	void shouldDeleteExpiredEventsInBatchesAndRecordMetric() {
		Instant now = Instant.parse("2026-08-10T04:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(365));
		ClickEventRetentionProperties properties =
				new ClickEventRetentionProperties(true, Duration.ofDays(365), 500);
		when(repository.deleteOldestBatch(cutoff, 500)).thenReturn(500, 31);
		ClickEventRetentionJob job = new ClickEventRetentionJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteExpiredEvents();

		verify(repository, times(2)).deleteOldestBatch(cutoff, 500);
		verify(metrics).recordDeleted(531);
	}

	@Test
	void shouldNotRecordMetricWhenNothingIsExpired() {
		Instant now = Instant.parse("2026-08-10T04:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(30));
		ClickEventRetentionProperties properties =
				new ClickEventRetentionProperties(true, Duration.ofDays(30), 100);
		when(repository.deleteOldestBatch(cutoff, 100)).thenReturn(0);
		ClickEventRetentionJob job = new ClickEventRetentionJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteExpiredEvents();

		verifyNoInteractions(metrics);
	}
}

package com.duong.url_shortener.auth;

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
class RegistrationCleanupJobTest {

	@Mock
	private RegistrationCleanupRepository repository;

	@Mock
	private RegistrationCleanupMetrics metrics;

	@Test
	void shouldDeleteAbandonedRegistrationsInBatchesAndRecordMetric() {
		Instant now = Instant.parse("2026-08-11T02:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(7));
		RegistrationCleanupProperties properties =
				new RegistrationCleanupProperties(true, Duration.ofDays(7), 500);
		when(repository.deleteOldestBatch(cutoff, 500)).thenReturn(500, 12);
		RegistrationCleanupJob job = new RegistrationCleanupJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteAbandonedRegistrations();

		verify(repository, times(2)).deleteOldestBatch(cutoff, 500);
		verify(metrics).recordDeleted(512);
	}

	@Test
	void shouldNotRecordMetricWhenNothingIsAbandoned() {
		Instant now = Instant.parse("2026-08-11T02:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(7));
		RegistrationCleanupProperties properties =
				new RegistrationCleanupProperties(true, Duration.ofDays(7), 100);
		when(repository.deleteOldestBatch(cutoff, 100)).thenReturn(0);
		RegistrationCleanupJob job = new RegistrationCleanupJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteAbandonedRegistrations();

		verifyNoInteractions(metrics);
	}
}

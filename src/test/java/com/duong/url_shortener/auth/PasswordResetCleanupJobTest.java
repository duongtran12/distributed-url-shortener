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
class PasswordResetCleanupJobTest {
	@Mock
	private PasswordResetTokenRepository repository;

	@Mock
	private PasswordResetCleanupMetrics metrics;

	@Test
	void shouldDeleteStaleTokensInBatchesAndRecordMetric() {
		Instant now = Instant.parse("2026-08-10T08:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(7));
		PasswordResetCleanupProperties properties =
				new PasswordResetCleanupProperties(true, Duration.ofDays(7), 500);
		when(repository.deleteStaleBatch(cutoff, 500)).thenReturn(500, 11);
		PasswordResetCleanupJob job = new PasswordResetCleanupJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteStaleTokens();

		verify(repository, times(2)).deleteStaleBatch(cutoff, 500);
		verify(metrics).recordDeleted(511);
	}

	@Test
	void shouldNotRecordMetricWhenNothingIsStale() {
		Instant now = Instant.parse("2026-08-10T08:00:00Z");
		Instant cutoff = now.minus(Duration.ofDays(7));
		PasswordResetCleanupProperties properties =
				new PasswordResetCleanupProperties(true, Duration.ofDays(7), 100);
		when(repository.deleteStaleBatch(cutoff, 100)).thenReturn(0);
		PasswordResetCleanupJob job = new PasswordResetCleanupJob(
				repository, properties, metrics, Clock.fixed(now, ZoneOffset.UTC));

		job.deleteStaleTokens();

		verifyNoInteractions(metrics);
	}
}

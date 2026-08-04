package com.duong.url_shortener.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisRateLimiterIntegrationTest {

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine")
			.withExposedPorts(6379);

	private static LettuceConnectionFactory firstConnectionFactory;
	private static LettuceConnectionFactory secondConnectionFactory;
	private static RedisRateLimiter firstInstance;
	private static RedisRateLimiter secondInstance;

	@BeforeAll
	static void setUpInstances() {
		firstConnectionFactory = connectionFactory();
		secondConnectionFactory = connectionFactory();
		RateLimitProperties properties = new RateLimitProperties(
				true, false, Duration.ofSeconds(30), 10, 60, 120);
		firstInstance = new RedisRateLimiter(
				new StringRedisTemplate(firstConnectionFactory), properties);
		secondInstance = new RedisRateLimiter(
				new StringRedisTemplate(secondConnectionFactory), properties);
	}

	@AfterAll
	static void closeConnections() {
		if (firstConnectionFactory != null) {
			firstConnectionFactory.destroy();
		}
		if (secondConnectionFactory != null) {
			secondConnectionFactory.destroy();
		}
	}

	@Test
	void shouldShareQuotaAcrossApplicationInstances() {
		String identity = "user:" + UUID.randomUUID();

		RateLimitDecision first = firstInstance.check("api", identity, 2);
		RateLimitDecision second = secondInstance.check("api", identity, 2);
		RateLimitDecision exceeded = firstInstance.check("api", identity, 2);

		assertTrue(first.allowed());
		assertEquals(1, first.remaining());
		assertTrue(second.allowed());
		assertEquals(0, second.remaining());
		assertFalse(exceeded.allowed());
		assertEquals(0, exceeded.remaining());
	}

	@Test
	void shouldKeepBucketsAndClientsIsolated() {
		String firstClient = "visitor:" + UUID.randomUUID();
		String secondClient = "visitor:" + UUID.randomUUID();

		assertTrue(firstInstance.check("auth", firstClient, 1).allowed());
		assertFalse(secondInstance.check("auth", firstClient, 1).allowed());
		assertTrue(secondInstance.check("auth", secondClient, 1).allowed());
		assertTrue(secondInstance.check("redirect", firstClient, 1).allowed());
	}

	private static LettuceConnectionFactory connectionFactory() {
		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
				REDIS.getHost(), REDIS.getMappedPort(6379));
		connectionFactory.afterPropertiesSet();
		return connectionFactory;
	}
}

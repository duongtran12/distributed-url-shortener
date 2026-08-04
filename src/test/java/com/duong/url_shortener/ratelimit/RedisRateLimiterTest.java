package com.duong.url_shortener.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisRateLimiterTest {

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

	@Test
	void shouldReturnRemainingQuotaFromRedisCounter() {
		when(redisTemplate.execute(any(), anyList(), any())).thenReturn(3L);
		RedisRateLimiter limiter = new RedisRateLimiter(
				redisTemplate,
				new RateLimitProperties(true, true, Duration.ofMinutes(1), 10, 60, 120));

		RateLimitDecision decision = limiter.check("api", "user:42", 10);

		assertTrue(decision.allowed());
		assertEquals(7, decision.remaining());
		assertEquals(60, decision.retryAfterSeconds());
	}

	@Test
	void shouldFailOpenWhenRedisIsUnavailable() {
		when(redisTemplate.execute(any(), anyList(), any()))
				.thenThrow(new RuntimeException("Redis unavailable"));
		RedisRateLimiter limiter = new RedisRateLimiter(
				redisTemplate,
				new RateLimitProperties(true, true, Duration.ofMinutes(1), 10, 60, 120));

		RateLimitDecision decision = limiter.check("redirect", "visitor:hash", 60);

		assertTrue(decision.allowed());
		assertEquals(0, decision.limit());
	}
}

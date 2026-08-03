package com.duong.url_shortener.shorturl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedirectCacheTest {

	private StringRedisTemplate redisTemplate;
	private ValueOperations<String, String> valueOperations;
	private RedirectCache redirectCache;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		redisTemplate = mock(StringRedisTemplate.class);
		valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		redirectCache = new RedirectCache(
				redisTemplate,
				new RedirectCacheProperties(true, Duration.ofHours(1)));
	}

	@Test
	void shouldReadCachedDestination() {
		when(valueOperations.get("redirect:abc1234")).thenReturn("https://example.com");

		assertEquals("https://example.com", redirectCache.get("abc1234").orElseThrow());
	}

	@Test
	void shouldLimitTtlToUrlExpiration() {
		Instant now = Instant.parse("2030-01-01T00:00:00Z");

		redirectCache.put(
				"abc1234",
				"https://example.com",
				now.plus(Duration.ofMinutes(10)),
				now);

		verify(valueOperations).set(
				eq("redirect:abc1234"),
				eq("https://example.com"),
				eq(Duration.ofMinutes(10)));
	}

	@Test
	void shouldNotCacheAlreadyExpiredUrl() {
		Instant now = Instant.parse("2030-01-01T00:00:00Z");

		redirectCache.put("expired", "https://example.com", now, now);

		verify(valueOperations, never()).set(any(), any(), any(Duration.class));
	}

	@Test
	void shouldTreatRedisFailureAsCacheMiss() {
		when(valueOperations.get("redirect:abc1234")).thenThrow(new RuntimeException("Redis unavailable"));

		assertTrue(redirectCache.get("abc1234").isEmpty());
		doThrow(new RuntimeException("Redis unavailable"))
				.when(redisTemplate).delete("redirect:abc1234");
		assertDoesNotThrow(() -> {
			redirectCache.evict("abc1234");
		});
	}

	@Test
	void shouldBypassRedisWhenCacheIsDisabled() {
		StringRedisTemplate disabledRedisTemplate = mock(StringRedisTemplate.class);
		RedirectCache disabledCache = new RedirectCache(
				disabledRedisTemplate,
				new RedirectCacheProperties(false, Duration.ofHours(1)));

		assertTrue(disabledCache.get("abc1234").isEmpty());
		disabledCache.put("abc1234", "https://example.com", null, Instant.now());
		disabledCache.evict("abc1234");

		verifyNoInteractions(disabledRedisTemplate);
	}
}

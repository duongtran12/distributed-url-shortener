package com.duong.url_shortener.shorturl;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedirectCache {

	private static final Logger log = LoggerFactory.getLogger(RedirectCache.class);
	private static final String KEY_PREFIX = "redirect:";

	private final StringRedisTemplate redisTemplate;
	private final RedirectCacheProperties properties;

	public RedirectCache(
			StringRedisTemplate redisTemplate,
			RedirectCacheProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public Optional<String> get(String shortCode) {
		if (!properties.enabled()) {
			return Optional.empty();
		}

		try {
			return Optional.ofNullable(redisTemplate.opsForValue().get(key(shortCode)));
		} catch (RuntimeException exception) {
			log.warn("Redis redirect cache read failed; falling back to PostgreSQL");
			return Optional.empty();
		}
	}

	public void put(String shortCode, String originalUrl, Instant expiresAt, Instant now) {
		if (!properties.enabled()) {
			return;
		}

		Duration ttl = properties.ttl();
		if (expiresAt != null) {
			Duration untilExpiration = Duration.between(now, expiresAt);
			if (untilExpiration.isNegative() || untilExpiration.isZero()) {
				return;
			}
			if (untilExpiration.compareTo(ttl) < 0) {
				ttl = untilExpiration;
			}
		}

		try {
			redisTemplate.opsForValue().set(key(shortCode), originalUrl, ttl);
		} catch (RuntimeException exception) {
			log.warn("Redis redirect cache write failed; continuing without cache");
		}
	}

	public void evict(String shortCode) {
		if (!properties.enabled()) {
			return;
		}

		try {
			redisTemplate.delete(key(shortCode));
		} catch (RuntimeException exception) {
			log.warn("Redis redirect cache eviction failed; cached value will expire by TTL");
		}
	}

	private String key(String shortCode) {
		return KEY_PREFIX + shortCode;
	}
}

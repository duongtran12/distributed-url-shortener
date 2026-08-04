package com.duong.url_shortener.ratelimit;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
	private static final String KEY_PREFIX = "rate-limit:";
	private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
			local current = redis.call('INCR', KEYS[1])
			if current == 1 then
			    redis.call('PEXPIRE', KEYS[1], ARGV[1])
			end
			return current
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final RateLimitProperties properties;

	public RedisRateLimiter(
			StringRedisTemplate redisTemplate,
			RateLimitProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public RateLimitDecision check(String bucket, String identity, int limit) {
		if (!properties.enabled()) {
			return RateLimitDecision.allowedWithoutLimit();
		}

		Duration window = properties.window();
		try {
			Long count = redisTemplate.execute(
					INCREMENT_SCRIPT,
					List.of(KEY_PREFIX + bucket + ":" + identity),
					Long.toString(window.toMillis()));
			long used = count == null ? 1 : count;
			return new RateLimitDecision(
					used <= limit,
					limit,
					Math.max(0, limit - used),
					Math.max(1, window.toSeconds()));
		} catch (RuntimeException exception) {
			if (properties.failOpen()) {
				log.warn("Redis rate limit check failed; allowing request");
				return RateLimitDecision.allowedWithoutLimit();
			}
			throw exception;
		}
	}
}

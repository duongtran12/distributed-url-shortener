package com.duong.url_shortener.shorturl;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import com.duong.url_shortener.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

	private final ShortUrlRepository shortUrlRepository;
	private final RedirectCache redirectCache;
	private final RedirectMetrics redirectMetrics;

	public RedirectService(
			ShortUrlRepository shortUrlRepository,
			RedirectCache redirectCache,
			RedirectMetrics redirectMetrics) {
		this.shortUrlRepository = shortUrlRepository;
		this.redirectCache = redirectCache;
		this.redirectMetrics = redirectMetrics;
	}

	@Transactional(readOnly = true)
	public URI resolve(String shortCode) {
		Optional<String> cachedUrl = redirectCache.get(shortCode);
		if (cachedUrl.isPresent()) {
			redirectMetrics.recordResolution("cache");
			return URI.create(cachedUrl.get());
		}

		ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
				.orElseThrow(() -> {
					redirectMetrics.recordFailure("not_found");
					return new ApiException(
							HttpStatus.NOT_FOUND,
							"SHORT_URL_NOT_FOUND",
							"Short URL does not exist");
				});

		Instant now = Instant.now();
		if (!shortUrl.isRedirectableAt(now)) {
			redirectMetrics.recordFailure("unavailable");
			throw new ApiException(
					HttpStatus.GONE,
					"SHORT_URL_UNAVAILABLE",
					"Short URL is disabled, blocked, or expired");
		}

		redirectCache.put(
				shortUrl.getShortCode(),
				shortUrl.getOriginalUrl(),
				shortUrl.getExpiresAt(),
				now);
		redirectMetrics.recordResolution("database");
		return URI.create(shortUrl.getOriginalUrl());
	}
}

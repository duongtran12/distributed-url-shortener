package com.duong.url_shortener.shorturl;

import java.net.URI;
import java.time.Instant;

import com.duong.url_shortener.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

	private final ShortUrlRepository shortUrlRepository;

	public RedirectService(ShortUrlRepository shortUrlRepository) {
		this.shortUrlRepository = shortUrlRepository;
	}

	@Transactional(readOnly = true)
	public URI resolve(String shortCode) {
		ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"SHORT_URL_NOT_FOUND",
						"Short URL does not exist"));

		if (!shortUrl.isRedirectableAt(Instant.now())) {
			throw new ApiException(
					HttpStatus.GONE,
					"SHORT_URL_UNAVAILABLE",
					"Short URL is disabled, blocked, or expired");
		}

		return URI.create(shortUrl.getOriginalUrl());
	}
}

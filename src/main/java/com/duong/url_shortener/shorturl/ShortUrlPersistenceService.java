package com.duong.url_shortener.shorturl;

import com.duong.url_shortener.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlPersistenceService {

	private final ShortUrlRepository shortUrlRepository;

	public ShortUrlPersistenceService(ShortUrlRepository shortUrlRepository) {
		this.shortUrlRepository = shortUrlRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ShortUrl create(
			User owner,
			String shortCode,
			String originalUrl,
			boolean customAlias,
			java.time.Instant expiresAt) {
		return shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, shortCode, originalUrl, customAlias, expiresAt));
	}
}

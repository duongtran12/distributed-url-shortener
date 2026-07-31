package com.duong.url_shortener.shorturl;

import java.time.Instant;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlService {

	private final UserRepository userRepository;
	private final ShortUrlInputValidator inputValidator;
	private final ShortCodeGenerator shortCodeGenerator;
	private final ShortUrlPersistenceService persistenceService;
	private final ShortUrlProperties properties;

	public ShortUrlService(
			UserRepository userRepository,
			ShortUrlInputValidator inputValidator,
			ShortCodeGenerator shortCodeGenerator,
			ShortUrlPersistenceService persistenceService,
			ShortUrlProperties properties) {
		this.userRepository = userRepository;
		this.inputValidator = inputValidator;
		this.shortCodeGenerator = shortCodeGenerator;
		this.persistenceService = persistenceService;
		this.properties = properties;
	}

	public ShortUrlResponse create(Long userId, CreateShortUrlRequest request) {
		User owner = findActiveUser(userId);
		String originalUrl = inputValidator.normalizeAndValidateOriginalUrl(request.originalUrl());
		String customAlias = inputValidator.normalizeAndValidateCustomAlias(request.customAlias());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());

		ShortUrl shortUrl = customAlias == null
				? createWithGeneratedCode(owner, originalUrl, request.expiresAt())
				: createWithCustomAlias(owner, originalUrl, customAlias, request.expiresAt());

		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	private ShortUrl createWithCustomAlias(
			User owner,
			String originalUrl,
			String customAlias,
			Instant expiresAt) {
		try {
			return persistenceService.create(owner, customAlias, originalUrl, true, expiresAt);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"CUSTOM_ALIAS_TAKEN",
					"Custom alias is already in use");
		}
	}

	private ShortUrl createWithGeneratedCode(User owner, String originalUrl, Instant expiresAt) {
		for (int attempt = 0; attempt < properties.maxGenerationAttempts(); attempt++) {
			try {
				return persistenceService.create(
						owner,
						shortCodeGenerator.generate(),
						originalUrl,
						false,
						expiresAt);
			} catch (DataIntegrityViolationException exception) {
				// A new transaction is used for each attempt because a constraint
				// violation marks the failed transaction for rollback.
			}
		}

		throw new ApiException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"SHORT_CODE_GENERATION_FAILED",
				"Could not allocate a unique short code");
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_ACCESS_TOKEN",
						"The access token no longer belongs to an existing user"));

		if (!user.isEnabled()) {
			throw new ApiException(
					HttpStatus.FORBIDDEN,
					"ACCOUNT_DISABLED",
					"The user account is disabled");
		}
		return user;
	}
}

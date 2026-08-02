package com.duong.url_shortener.shorturl;

import java.time.Instant;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

	private final UserRepository userRepository;
	private final ShortUrlRepository shortUrlRepository;
	private final ShortUrlInputValidator inputValidator;
	private final ShortCodeGenerator shortCodeGenerator;
	private final ShortUrlPersistenceService persistenceService;
	private final ShortUrlProperties properties;

	public ShortUrlService(
			UserRepository userRepository,
			ShortUrlRepository shortUrlRepository,
			ShortUrlInputValidator inputValidator,
			ShortCodeGenerator shortCodeGenerator,
			ShortUrlPersistenceService persistenceService,
			ShortUrlProperties properties) {
		this.userRepository = userRepository;
		this.shortUrlRepository = shortUrlRepository;
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

	@Transactional(readOnly = true)
	public ShortUrlPageResponse findAllOwnedBy(Long userId, int page, int size) {
		findActiveUser(userId);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt"));
		return ShortUrlPageResponse.from(
				shortUrlRepository.findAllByOwnerId(userId, pageRequest),
				properties.baseUrl());
	}

	@Transactional(readOnly = true)
	public ShortUrlResponse findOwnedById(Long userId, Long shortUrlId) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);
		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional
	public ShortUrlResponse updateStatus(
			Long userId,
			Long shortUrlId,
			UpdateShortUrlStatusRequest request) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);

		switch (request.status()) {
			case ACTIVE -> shortUrl.enable();
			case DISABLED -> shortUrl.disable();
		}

		if (shortUrl.getStatus() == ShortUrlStatus.BLOCKED) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"SHORT_URL_BLOCKED",
					"A blocked short URL cannot be modified by its owner");
		}

		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional
	public ShortUrlResponse update(
			Long userId,
			Long shortUrlId,
			UpdateShortUrlRequest request) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);

		if (shortUrl.getStatus() == ShortUrlStatus.BLOCKED) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"SHORT_URL_BLOCKED",
					"A blocked short URL cannot be modified by its owner");
		}

		String originalUrl = inputValidator.normalizeAndValidateOriginalUrl(request.originalUrl());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());
		shortUrl.updateDestination(originalUrl, request.expiresAt());
		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional
	public void delete(Long userId, Long shortUrlId) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);
		shortUrlRepository.delete(shortUrl);
	}

	private ShortUrl findOwnedShortUrl(Long userId, Long shortUrlId) {
		return shortUrlRepository.findByIdAndOwnerId(shortUrlId, userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"SHORT_URL_NOT_FOUND",
						"Short URL was not found"));
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

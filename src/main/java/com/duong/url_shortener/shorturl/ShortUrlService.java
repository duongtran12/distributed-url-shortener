package com.duong.url_shortener.shorturl;

import java.time.Instant;
import java.util.Locale;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

	private final UserRepository userRepository;
	private final ShortUrlRepository shortUrlRepository;
	private final RedirectCache redirectCache;
	private final ShortUrlInputValidator inputValidator;
	private final ShortCodeGenerator shortCodeGenerator;
	private final ShortUrlPersistenceService persistenceService;
	private final ShortUrlProperties properties;

	public ShortUrlService(
			UserRepository userRepository,
			ShortUrlRepository shortUrlRepository,
			RedirectCache redirectCache,
			ShortUrlInputValidator inputValidator,
			ShortCodeGenerator shortCodeGenerator,
			ShortUrlPersistenceService persistenceService,
			ShortUrlProperties properties) {
		this.userRepository = userRepository;
		this.shortUrlRepository = shortUrlRepository;
		this.redirectCache = redirectCache;
		this.inputValidator = inputValidator;
		this.shortCodeGenerator = shortCodeGenerator;
		this.persistenceService = persistenceService;
		this.properties = properties;
	}

	public ShortUrlResponse create(Long userId, CreateShortUrlRequest request) {
		User owner = findActiveUser(userId);
		String originalUrl = inputValidator.normalizeAndValidateOriginalUrl(request.originalUrl());
		String title = normalizeTitle(request.title());
		String customAlias = inputValidator.normalizeAndValidateCustomAlias(request.customAlias());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());

		ShortUrl shortUrl = customAlias == null
				? createWithGeneratedCode(owner, originalUrl, title, request.expiresAt())
				: createWithCustomAlias(owner, originalUrl, title, customAlias, request.expiresAt());

		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional(readOnly = true)
	public ShortUrlPageResponse findAllOwnedBy(
			Long userId,
			int page,
			int size,
			String query,
			ShortUrlStatus status) {
		findActiveUser(userId);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createdAt"));
		Specification<ShortUrl> filters = ownedBy(userId);
		String normalizedQuery = normalizeSearchQuery(query);
		if (normalizedQuery != null) {
			filters = filters.and(matchesSearchQuery(normalizedQuery));
		}
		if (status != null) {
			filters = filters.and(hasStatus(status));
		}

		return ShortUrlPageResponse.from(shortUrlRepository.findAll(filters, pageRequest), properties.baseUrl());
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

		redirectCache.evict(shortUrl.getShortCode());
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
		String title = normalizeTitle(request.title());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());
		shortUrl.updateDetails(originalUrl, title, request.expiresAt());
		redirectCache.evict(shortUrl.getShortCode());
		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional
	public void delete(Long userId, Long shortUrlId) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);
		shortUrlRepository.delete(shortUrl);
		redirectCache.evict(shortUrl.getShortCode());
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
			String title,
			String customAlias,
			Instant expiresAt) {
		try {
			return persistenceService.create(owner, customAlias, originalUrl, title, true, expiresAt);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"CUSTOM_ALIAS_TAKEN",
					"Custom alias is already in use");
		}
	}

	private ShortUrl createWithGeneratedCode(
			User owner,
			String originalUrl,
			String title,
			Instant expiresAt) {
		for (int attempt = 0; attempt < properties.maxGenerationAttempts(); attempt++) {
			try {
				return persistenceService.create(
						owner,
						shortCodeGenerator.generate(),
						originalUrl,
						title,
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

	private Specification<ShortUrl> ownedBy(Long userId) {
		return (root, criteriaQuery, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("owner").get("id"), userId);
	}

	private Specification<ShortUrl> matchesSearchQuery(String query) {
		String pattern = "%" + escapeLikePattern(query) + "%";
		return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
				criteriaBuilder.like(criteriaBuilder.lower(root.get("shortCode")), pattern, '\\'),
				criteriaBuilder.like(criteriaBuilder.lower(root.get("originalUrl")), pattern, '\\'),
				criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '\\'));
	}

	private Specification<ShortUrl> hasStatus(ShortUrlStatus status) {
		return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
	}

	private String normalizeSearchQuery(String query) {
		if (query == null || query.isBlank()) {
			return null;
		}
		return query.trim().toLowerCase(Locale.ROOT);
	}

	private String escapeLikePattern(String query) {
		return query.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}

	private String normalizeTitle(String title) {
		if (title == null || title.isBlank()) {
			return null;
		}
		return title.trim();
	}
}

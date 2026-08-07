package com.duong.url_shortener.shorturl;

import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
		String tag = normalizeTag(request.tag());
		String customAlias = inputValidator.normalizeAndValidateCustomAlias(request.customAlias());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());

		ShortUrl shortUrl = customAlias == null
				? createWithGeneratedCode(owner, originalUrl, title, tag, request.expiresAt())
				: createWithCustomAlias(owner, originalUrl, title, tag, customAlias, request.expiresAt());

		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	@Transactional(readOnly = true)
	public ShortUrlPageResponse findAllOwnedBy(
			Long userId,
			int page,
			int size,
			String query,
			String tag,
			ShortUrlStatus status,
			ShortUrlSort sort) {
		findActiveUser(userId);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				sort.toSort());
		Specification<ShortUrl> filters = ownedBy(userId);
		String normalizedQuery = normalizeSearchQuery(query);
		if (normalizedQuery != null) {
			filters = filters.and(matchesSearchQuery(normalizedQuery));
		}
		if (status != null) {
			filters = filters.and(hasStatus(status));
		}
		String normalizedTag = normalizeTag(tag);
		if (normalizedTag != null) {
			filters = filters.and(hasTag(normalizedTag));
		}

		return ShortUrlPageResponse.from(shortUrlRepository.findAll(filters, pageRequest), properties.baseUrl());
	}

	@Transactional(readOnly = true)
	public ShortUrlResponse findOwnedById(Long userId, Long shortUrlId) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);
		return ShortUrlResponse.from(shortUrl, properties.baseUrl());
	}

	public ShortUrlResponse duplicate(Long userId, Long shortUrlId) {
		User owner = findActiveUser(userId);
		ShortUrl source = findOwnedShortUrl(userId, shortUrlId);
		if (source.getStatus() == ShortUrlStatus.BLOCKED) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"SHORT_URL_BLOCKED",
					"A blocked short URL cannot be duplicated by its owner");
		}

		Instant now = Instant.now();
		Instant expiresAt = source.getExpiresAt() != null && source.getExpiresAt().isAfter(now)
				? source.getExpiresAt()
				: null;
		ShortUrl duplicate = createWithGeneratedCode(
				owner,
				source.getOriginalUrl(),
				source.getTitle(),
				source.getTag(),
				expiresAt);
		return ShortUrlResponse.from(duplicate, properties.baseUrl());
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
	public ShortUrlResponse updatePin(
			Long userId,
			Long shortUrlId,
			UpdateShortUrlPinRequest request) {
		findActiveUser(userId);
		ShortUrl shortUrl = findOwnedShortUrl(userId, shortUrlId);
		shortUrl.setPinned(request.pinned());
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
		String tag = normalizeTag(request.tag());
		inputValidator.validateExpiration(request.expiresAt(), Instant.now());
		shortUrl.updateDetails(originalUrl, title, tag, request.expiresAt());
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

	@Transactional
	public BulkShortUrlResponse bulkUpdate(Long userId, BulkShortUrlRequest request) {
		findActiveUser(userId);
		String bulkTag = normalizeTag(request.tag());
		if (request.action() == BulkShortUrlRequest.Action.SET_TAG && bulkTag == null) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"BULK_TAG_REQUIRED",
					"A tag is required for the SET_TAG action");
		}
		List<Long> ids = List.copyOf(new LinkedHashSet<>(request.ids()));
		List<ShortUrl> shortUrls = shortUrlRepository.findAllByIdInAndOwnerId(ids, userId);
		if (shortUrls.size() != ids.size()) {
			throw new ApiException(
					HttpStatus.NOT_FOUND,
					"SHORT_URL_NOT_FOUND",
					"One or more short URLs were not found");
		}

		if (request.action() != BulkShortUrlRequest.Action.DELETE
				&& shortUrls.stream().anyMatch(shortUrl -> shortUrl.getStatus() == ShortUrlStatus.BLOCKED)) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"SHORT_URL_BLOCKED",
					"A blocked short URL cannot be modified by its owner");
		}

		for (ShortUrl shortUrl : shortUrls) {
			switch (request.action()) {
				case ENABLE -> shortUrl.enable();
				case DISABLE -> shortUrl.disable();
				case DELETE -> shortUrlRepository.delete(shortUrl);
				case SET_TAG -> shortUrl.setTag(bulkTag);
				case CLEAR_TAG -> shortUrl.setTag(null);
			}
			redirectCache.evict(shortUrl.getShortCode());
		}
		return new BulkShortUrlResponse(request.action(), shortUrls.size());
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
			String tag,
			String customAlias,
			Instant expiresAt) {
		try {
			return persistenceService.create(owner, customAlias, originalUrl, title, tag, true, expiresAt);
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
			String tag,
			Instant expiresAt) {
		for (int attempt = 0; attempt < properties.maxGenerationAttempts(); attempt++) {
			try {
				return persistenceService.create(
						owner,
						shortCodeGenerator.generate(),
						originalUrl,
						title,
						tag,
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
				criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '\\'),
				criteriaBuilder.like(criteriaBuilder.lower(root.get("tag")), pattern, '\\'));
	}

	private Specification<ShortUrl> hasStatus(ShortUrlStatus status) {
		return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
	}

	private Specification<ShortUrl> hasTag(String tag) {
		return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("tag"), tag);
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

	private String normalizeTag(String tag) {
		if (tag == null || tag.isBlank()) {
			return null;
		}
		return tag.trim().toLowerCase(Locale.ROOT);
	}
}

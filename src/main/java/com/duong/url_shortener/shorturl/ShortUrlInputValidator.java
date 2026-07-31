package com.duong.url_shortener.shorturl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.duong.url_shortener.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlInputValidator {

	private static final int MAX_URL_LENGTH = 2048;
	private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("[A-Za-z0-9_-]{4,32}");
	private static final Set<String> RESERVED_ALIASES = Set.of(
			"api",
			"admin",
			"login",
			"register",
			"swagger-ui",
			"actuator",
			"health",
			"metrics",
			"docs",
			"favicon.ico");

	public String normalizeAndValidateOriginalUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			throw badRequest("INVALID_ORIGINAL_URL", "Original URL must not be blank");
		}

		String normalizedUrl = originalUrl.strip();
		if (normalizedUrl.length() > MAX_URL_LENGTH) {
			throw badRequest(
					"INVALID_ORIGINAL_URL",
					"Original URL must not exceed " + MAX_URL_LENGTH + " characters");
		}

		URI uri;
		try {
			uri = new URI(normalizedUrl);
		} catch (URISyntaxException exception) {
			throw badRequest("INVALID_ORIGINAL_URL", "Original URL is not a valid URI");
		}

		String scheme = uri.getScheme();
		if (scheme == null
				|| (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
			throw badRequest("UNSUPPORTED_URL_SCHEME", "Only HTTP and HTTPS URLs are supported");
		}

		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw badRequest("INVALID_ORIGINAL_URL", "Original URL must contain a valid host");
		}

		return normalizedUrl;
	}

	public String normalizeAndValidateCustomAlias(String customAlias) {
		if (customAlias == null) {
			return null;
		}

		String normalizedAlias = customAlias.strip();
		if (!CUSTOM_ALIAS_PATTERN.matcher(normalizedAlias).matches()) {
			throw badRequest(
					"INVALID_CUSTOM_ALIAS",
					"Custom alias must contain 4 to 32 letters, numbers, hyphens, or underscores");
		}

		if (RESERVED_ALIASES.contains(normalizedAlias.toLowerCase(Locale.ROOT))) {
			throw badRequest("RESERVED_CUSTOM_ALIAS", "Custom alias is reserved by the application");
		}

		return normalizedAlias;
	}

	public void validateExpiration(Instant expiresAt, Instant now) {
		if (expiresAt != null && !expiresAt.isAfter(now)) {
			throw badRequest("INVALID_EXPIRATION", "Expiration time must be in the future");
		}
	}

	private ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}
}

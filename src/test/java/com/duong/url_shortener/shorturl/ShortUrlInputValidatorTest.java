package com.duong.url_shortener.shorturl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.duong.url_shortener.common.exception.ApiException;
import org.junit.jupiter.api.Test;

class ShortUrlInputValidatorTest {

	private final ShortUrlInputValidator validator = new ShortUrlInputValidator();

	@Test
	void shouldAcceptAndNormalizeHttpAndHttpsUrls() {
		assertThat(validator.normalizeAndValidateOriginalUrl(" https://example.com/a?q=1#section "))
				.isEqualTo("https://example.com/a?q=1#section");
		assertThat(validator.normalizeAndValidateOriginalUrl("http://localhost:8080/path"))
				.isEqualTo("http://localhost:8080/path");
	}

	@Test
	void shouldRejectUnsafeOrIncompleteUrls() {
		assertApiError(
				() -> validator.normalizeAndValidateOriginalUrl("javascript:alert(1)"),
				"UNSUPPORTED_URL_SCHEME");
		assertApiError(
				() -> validator.normalizeAndValidateOriginalUrl("file:///etc/passwd"),
				"UNSUPPORTED_URL_SCHEME");
		assertApiError(
				() -> validator.normalizeAndValidateOriginalUrl("https:///missing-host"),
				"INVALID_ORIGINAL_URL");
		assertApiError(
				() -> validator.normalizeAndValidateOriginalUrl("example.com/path"),
				"UNSUPPORTED_URL_SCHEME");
	}

	@Test
	void shouldAcceptValidCaseSensitiveCustomAlias() {
		assertThat(validator.normalizeAndValidateCustomAlias(" Java_Roadmap-21 "))
				.isEqualTo("Java_Roadmap-21");
		assertThat(validator.normalizeAndValidateCustomAlias(null)).isNull();
	}

	@Test
	void shouldRejectMalformedOrReservedCustomAlias() {
		assertApiError(
				() -> validator.normalizeAndValidateCustomAlias("abc"),
				"INVALID_CUSTOM_ALIAS");
		assertApiError(
				() -> validator.normalizeAndValidateCustomAlias("java/roadmap"),
				"INVALID_CUSTOM_ALIAS");
		assertApiError(
				() -> validator.normalizeAndValidateCustomAlias("AcTuAtOr"),
				"RESERVED_CUSTOM_ALIAS");
	}

	@Test
	void shouldRequireExpirationToBeInTheFuture() {
		Instant now = Instant.parse("2026-07-31T00:00:00Z");

		validator.validateExpiration(null, now);
		validator.validateExpiration(now.plusSeconds(1), now);
		assertApiError(
				() -> validator.validateExpiration(now, now),
				"INVALID_EXPIRATION");
		assertApiError(
				() -> validator.validateExpiration(now.minusSeconds(1), now),
				"INVALID_EXPIRATION");
	}

	private void assertApiError(Runnable action, String expectedCode) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(
						ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(expectedCode));
	}
}

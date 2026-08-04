package com.duong.url_shortener.shorturl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(properties = "debug=false")
@ActiveProfiles("test")
@Testcontainers
class ShortUrlRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	@Autowired
	private UserRepository userRepository;

	private User owner;

	@BeforeEach
	void setUpOwner() {
		owner = userRepository.saveAndFlush(
				User.create("owner@example.com", "encoded-password", "URL Owner"));
	}

	@Test
	void shouldPersistShortUrlWithOwnerAndDefaultState() {
		Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

		ShortUrl saved = shortUrlRepository.saveAndFlush(ShortUrl.create(
				owner,
				"a8Kd91Z",
				"https://example.com/articles/spring-boot",
				false,
				expiresAt));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
		assertThat(saved.getStatus()).isEqualTo(ShortUrlStatus.ACTIVE);
		assertThat(saved.isCustomAlias()).isFalse();
		assertThat(saved.isRedirectableAt(Instant.now())).isTrue();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(shortUrlRepository.findByShortCode("a8Kd91Z")).contains(saved);
	}

	@Test
	void shouldRejectDuplicateShortCodeAtDatabaseBoundary() {
		shortUrlRepository.saveAndFlush(ShortUrl.create(
				owner,
				"java101",
				"https://example.com/first",
				true,
				null));

		assertThatThrownBy(() -> shortUrlRepository.saveAndFlush(ShortUrl.create(
				owner,
				"java101",
				"https://example.com/second",
				true,
				null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldDisableOnlyActiveExpiredUrls() {
		Instant createdAround = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant expiresBeforeCleanup = createdAround.plus(1, ChronoUnit.MINUTES);
		Instant cleanupTime = createdAround.plus(2, ChronoUnit.MINUTES);
		ShortUrl expired = shortUrlRepository.save(ShortUrl.create(
				owner,
				"expired1",
				"https://example.com/expired",
				false,
				expiresBeforeCleanup));
		ShortUrl future = shortUrlRepository.save(ShortUrl.create(
				owner,
				"future01",
				"https://example.com/future",
				false,
				cleanupTime.plus(1, ChronoUnit.DAYS)));
		ShortUrl permanent = shortUrlRepository.save(ShortUrl.create(
				owner,
				"forever1",
				"https://example.com/permanent",
				false,
				null));
		ShortUrl blockedExpired = ShortUrl.create(
				owner,
				"blocked1",
				"https://example.com/blocked",
				false,
				expiresBeforeCleanup);
		blockedExpired.block();
		shortUrlRepository.saveAndFlush(blockedExpired);

		int updated = shortUrlRepository.disableExpiredUrls(
				cleanupTime,
				ShortUrlStatus.ACTIVE,
				ShortUrlStatus.DISABLED);

		assertThat(updated).isEqualTo(1);
		assertThat(shortUrlRepository.findById(expired.getId()).orElseThrow().getStatus())
				.isEqualTo(ShortUrlStatus.DISABLED);
		assertThat(shortUrlRepository.findById(future.getId()).orElseThrow().getStatus())
				.isEqualTo(ShortUrlStatus.ACTIVE);
		assertThat(shortUrlRepository.findById(permanent.getId()).orElseThrow().getStatus())
				.isEqualTo(ShortUrlStatus.ACTIVE);
		assertThat(shortUrlRepository.findById(blockedExpired.getId()).orElseThrow().getStatus())
				.isEqualTo(ShortUrlStatus.BLOCKED);
	}
}

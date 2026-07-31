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
}

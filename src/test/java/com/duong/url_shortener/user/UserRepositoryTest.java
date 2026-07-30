package com.duong.url_shortener.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class UserRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private UserRepository userRepository;

	@Test
	void shouldPersistUserWithNormalizedEmailAndDefaultState() {
		User saved = userRepository.saveAndFlush(
				User.create("  Student@Example.COM ", "encoded-password", "Student User"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getEmail()).isEqualTo("student@example.com");
		assertThat(saved.getRole()).isEqualTo(Role.USER);
		assertThat(saved.isEnabled()).isTrue();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(userRepository.findByEmail("student@example.com")).contains(saved);
	}

	@Test
	void shouldRejectDuplicateEmailAtDatabaseBoundary() {
		userRepository.saveAndFlush(User.create("student@example.com", "first-password", "First User"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(
				User.create("STUDENT@example.com", "second-password", "Second User")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}

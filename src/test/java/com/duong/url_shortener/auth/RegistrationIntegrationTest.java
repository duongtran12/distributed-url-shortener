package com.duong.url_shortener.auth;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RegistrationIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailVerificationTokenRepository tokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RegistrationCleanupRepository registrationCleanupRepository;

	@MockitoBean
	private JavaMailSender mailSender;

	@BeforeEach
	void cleanDatabase() {
		tokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldRegisterUserAndHashPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "  Student@Example.COM ",
						  "password": "strong-password",
						  "displayName": "Student User"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("student@example.com"))
				.andExpect(jsonPath("$.displayName").value("Student User"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		User saved = userRepository.findByEmail("student@example.com").orElseThrow();
		assertThat(saved.getPasswordHash()).isNotEqualTo("strong-password");
		assertThat(passwordEncoder.matches("strong-password", saved.getPasswordHash())).isTrue();
		assertThat(saved.isEnabled()).isFalse();
		assertThat(tokenRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"student@example.com","password":"strong-password"}
						"""))
				.andExpect(status().isUnauthorized());

		ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		String token = extractToken(messageCaptor.getValue().getText());
		String storedHash = jdbcTemplate.queryForObject(
				"SELECT token_hash FROM email_verification_tokens", String.class);
		assertThat(storedHash).hasSize(64).isNotEqualTo(token);

		mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s"}
						""".formatted(token)))
				.andExpect(status().isNoContent());

		assertThat(userRepository.findByEmail("student@example.com").orElseThrow().isEnabled()).isTrue();
		assertThat(tokenRepository.count()).isZero();
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"student@example.com","password":"strong-password"}
						"""))
				.andExpect(status().isOk());
	}

	@Test
	void shouldRejectDuplicateEmailIgnoringCaseAndWhitespace() throws Exception {
		userRepository.saveAndFlush(User.create("student@example.com", "encoded-password", "Existing User"));

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": " STUDENT@EXAMPLE.COM ",
						  "password": "strong-password",
						  "displayName": "Another User"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void shouldRejectInvalidRegistrationRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "not-an-email",
						  "password": "short",
						  "displayName": ""
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'displayName')]").exists());
	}

	@Test
	void shouldNotRevealWhetherVerificationEmailExists() throws Exception {
		mockMvc.perform(post("/api/v1/auth/email-verification/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"missing@example.com"}
						"""))
				.andExpect(status().isAccepted());

		verifyNoInteractions(mailSender);
		assertThat(tokenRepository.count()).isZero();
	}

	@Test
	void shouldDeleteOnlyAbandonedUnverifiedRegistrations() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email":"abandoned@example.com",
						  "password":"strong-password",
						  "displayName":"Abandoned User"
						}
						"""))
				.andExpect(status().isCreated());
		userRepository.saveAndFlush(User.create(
				"verified@example.com", "encoded-password", "Verified User"));
		Instant oldCreationTime = Instant.parse("2026-07-01T00:00:00Z");
		jdbcTemplate.update("UPDATE users SET created_at = ?", Timestamp.from(oldCreationTime));

		int deleted = registrationCleanupRepository.deleteOldestBatch(
				Instant.parse("2026-08-04T00:00:00Z"), 100);

		assertThat(deleted).isEqualTo(1);
		assertThat(userRepository.findByEmail("abandoned@example.com")).isEmpty();
		assertThat(userRepository.findByEmail("verified@example.com")).isPresent();
		assertThat(tokenRepository.count()).isZero();
	}

	private String extractToken(String body) {
		assertThat(body).isNotNull();
		String marker = "?verificationToken=";
		int start = body.indexOf(marker);
		assertThat(start).isGreaterThanOrEqualTo(0);
		return body.substring(start + marker.length()).split("\\s", 2)[0];
	}
}

package com.duong.url_shortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PasswordResetIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordResetTokenRepository tokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private JavaMailSender mailSender;

	@BeforeEach
	void setUpUser() {
		tokenRepository.deleteAll();
		userRepository.deleteAll();
		userRepository.saveAndFlush(User.create(
				"student@example.com",
				passwordEncoder.encode("strong-password"),
				"Student User"));
	}

	@Test
	void shouldResetPasswordOnceAndRevokeExistingSessions() throws Exception {
		MvcResult login = login("strong-password").andExpect(status().isOk()).andReturn();
		Cookie refreshToken = login.getResponse().getCookie("shortwave_refresh");

		mockMvc.perform(post("/api/v1/auth/password-reset/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":" STUDENT@EXAMPLE.COM "}
						"""))
				.andExpect(status().isAccepted());

		ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		SimpleMailMessage message = messageCaptor.getValue();
		assertThat(message.getTo()).containsExactly("student@example.com");
		assertThat(message.getSubject()).isEqualTo("Reset your Shortwave password");
		String token = extractToken(message.getText());

		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s","newPassword":"new-strong-password"}
						""".formatted(token)))
				.andExpect(status().isNoContent());

		login("strong-password").andExpect(status().isUnauthorized());
		login("new-strong-password").andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshToken))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s","newPassword":"another-password"}
						""".formatted(token)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
	}

	@Test
	void shouldNotRevealWhetherRequestedEmailExists() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"missing@example.com"}
						"""))
				.andExpect(status().isAccepted());

		verifyNoInteractions(mailSender);
		assertThat(tokenRepository.count()).isZero();
	}

	private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"student@example.com","password":"%s"}
						""".formatted(password)));
	}

	private String extractToken(String body) {
		assertThat(body).isNotNull();
		String marker = "?resetToken=";
		int start = body.indexOf(marker);
		assertThat(start).isGreaterThanOrEqualTo(0);
		return body.substring(start + marker.length()).split("\\s", 2)[0];
	}
}

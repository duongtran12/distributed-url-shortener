package com.duong.url_shortener.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UserProfileIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUpUser() {
		userRepository.deleteAll();
		userRepository.saveAndFlush(User.create(
				"student@example.com",
				passwordEncoder.encode("strong-password"),
				"Student User"));
	}

	@Test
	void shouldReturnCurrentUserProfile() throws Exception {
		String accessToken = login();

		mockMvc.perform(get("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("student@example.com"))
				.andExpect(jsonPath("$.displayName").value("Student User"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist());
	}

	@Test
	void shouldRejectProfileRequestWithoutAccessToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectPreviouslyIssuedTokenAfterAccountIsDisabled() throws Exception {
		String accessToken = login();
		User user = userRepository.findByEmail("student@example.com").orElseThrow();
		user.disable();
		userRepository.saveAndFlush(user);

		mockMvc.perform(get("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
	}

	@Test
	void shouldChangePasswordAndRejectTheOldPassword() throws Exception {
		String accessToken = login();

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "currentPassword": "strong-password",
						  "newPassword": "new-strong-password"
						}
						"""))
				.andExpect(status().isNoContent());

		loginWithPassword("strong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		loginWithPassword("new-strong-password")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void shouldRejectIncorrectCurrentPassword() throws Exception {
		String accessToken = login();

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "currentPassword": "incorrect-password",
						  "newPassword": "new-strong-password"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
	}

	@Test
	void shouldRejectReusingCurrentPassword() throws Exception {
		String accessToken = login();

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "currentPassword": "strong-password",
						  "newPassword": "strong-password"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
	}

	private String login() throws Exception {
		String response = loginWithPassword("strong-password")
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return JsonPath.read(response, "$.accessToken");
	}

	private ResultActions loginWithPassword(String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "student@example.com",
						  "password": "%s"
						}
						""".formatted(password)));
	}
}

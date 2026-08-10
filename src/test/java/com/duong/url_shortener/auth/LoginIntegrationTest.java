package com.duong.url_shortener.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LoginIntegrationTest {

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
	void shouldLoginAndUseAccessTokenForProtectedEndpoint() throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": " STUDENT@EXAMPLE.COM ",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String accessToken = JsonPath.read(response, "$.accessToken");

		mockMvc.perform(get("/actuator/info")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	@Test
	void shouldRotateRefreshTokenRejectReplayAndRevokeOnLogout() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "student@example.com",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		Cookie firstRefreshToken = login.getResponse().getCookie("shortwave_refresh");
		assertNotNull(firstRefreshToken);
		assertTrue(firstRefreshToken.isHttpOnly());
		assertTrue(firstRefreshToken.getMaxAge() > 0);

		MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(firstRefreshToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();
		Cookie rotatedRefreshToken = refresh.getResponse().getCookie("shortwave_refresh");
		assertNotNull(rotatedRefreshToken);
		assertNotEquals(firstRefreshToken.getValue(), rotatedRefreshToken.getValue());

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		mockMvc.perform(post("/api/v1/auth/logout").cookie(rotatedRefreshToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void shouldRevokeRefreshSessionsWhenPasswordChanges() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"student@example.com","password":"strong-password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
		Cookie refreshToken = login.getResponse().getCookie("shortwave_refresh");
		assertNotNull(refreshToken);

		mockMvc.perform(patch("/api/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"currentPassword":"strong-password","newPassword":"new-strong-password"}
						"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void shouldListOwnedSessionsMarkCurrentAndRevokeAnotherSession() throws Exception {
		MvcResult firstLogin = loginWithUserAgent("Desktop browser");
		MvcResult secondLogin = loginWithUserAgent("Mobile browser");
		String accessToken = JsonPath.read(secondLogin.getResponse().getContentAsString(), "$.accessToken");
		Cookie currentCookie = secondLogin.getResponse().getCookie("shortwave_refresh");
		assertNotNull(currentCookie);

		String sessions = mockMvc.perform(get("/api/v1/auth/sessions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.cookie(currentCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].userAgent").value("Mobile browser"))
				.andExpect(jsonPath("$[0].current").value(true))
				.andExpect(jsonPath("$[1].userAgent").value("Desktop browser"))
				.andExpect(jsonPath("$[1].current").value(false))
				.andReturn().getResponse().getContentAsString();
		Number firstSessionId = JsonPath.read(sessions, "$[1].id");

		mockMvc.perform(delete("/api/v1/auth/sessions/{id}", firstSessionId.longValue())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.cookie(currentCookie))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(firstLogin.getResponse().getCookie("shortwave_refresh")))
				.andExpect(status().isUnauthorized());
	}

	private MvcResult loginWithUserAgent(String userAgent) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.header(HttpHeaders.USER_AGENT, userAgent)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"student@example.com","password":"strong-password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();
	}

	@Test
	void shouldRejectInvalidPasswordWithoutRevealingAccountDetails() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "student@example.com",
						  "password": "wrong-password"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void shouldRejectDisabledUser() throws Exception {
		User user = userRepository.findByEmail("student@example.com").orElseThrow();
		user.disable();
		userRepository.saveAndFlush(user);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "student@example.com",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void shouldRejectProtectedRequestWithoutAccessToken() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized());
	}
}

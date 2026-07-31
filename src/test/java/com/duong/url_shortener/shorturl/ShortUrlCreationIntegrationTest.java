package com.duong.url_shortener.shorturl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duong.url_shortener.security.JwtTokenService;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ShortUrlCreationIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	private String accessToken;

	@BeforeEach
	void setUpUser() {
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		User user = userRepository.saveAndFlush(
				User.create("owner@example.com", "encoded-password", "URL Owner"));
		accessToken = jwtTokenService.createAccessToken(user);
	}

	@Test
	void shouldCreateShortUrlWithGeneratedCode() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "https://example.com/long/path",
						  "expiresAt": "2099-12-31T23:59:59Z"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.shortCode").value(
						org.hamcrest.Matchers.matchesPattern("[0-9A-Za-z]{7}")))
				.andExpect(jsonPath("$.shortUrl").value(
						org.hamcrest.Matchers.startsWith("http://localhost:8080/")))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/long/path"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.customAlias").value(false));
	}

	@Test
	void shouldCreateShortUrlWithCustomAliasAndRejectDuplicate() throws Exception {
		String request = """
				{
				  "originalUrl": "https://example.com/java",
				  "customAlias": "Java_Roadmap"
				}
				""";

		mockMvc.perform(post("/api/v1/urls")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").value("Java_Roadmap"))
				.andExpect(jsonPath("$.customAlias").value(true));

		mockMvc.perform(post("/api/v1/urls")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CUSTOM_ALIAS_TAKEN"));
	}

	@Test
	void shouldRejectInvalidInput() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "originalUrl": "javascript:alert(1)",
						  "customAlias": "actuator"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_URL_SCHEME"));
	}

	@Test
	void shouldRequireAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"originalUrl": "https://example.com"}
						"""))
				.andExpect(status().isUnauthorized());
	}
}

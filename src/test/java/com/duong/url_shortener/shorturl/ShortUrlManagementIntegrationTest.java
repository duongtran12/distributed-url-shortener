package com.duong.url_shortener.shorturl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ShortUrlManagementIntegrationTest {

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

	private User owner;
	private User otherUser;
	private String ownerToken;

	@BeforeEach
	void setUp() {
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		owner = userRepository.saveAndFlush(
				User.create("owner@example.com", "encoded-password", "Owner"));
		otherUser = userRepository.saveAndFlush(
				User.create("other@example.com", "encoded-password", "Other"));
		ownerToken = jwtTokenService.createAccessToken(owner);
	}

	@Test
	void shouldListOnlyOwnedUrlsWithPagination() throws Exception {
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned01", "https://example.com/one", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned02", "https://example.com/two", false, null));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign", "https://example.com/foreign", false, null));

		mockMvc.perform(get("/api/v1/urls")
				.param("page", "0")
				.param("size", "1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void shouldReturnOwnedUrlDetails() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "details", "https://example.com/details", true, null));

		mockMvc.perform(get("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(shortUrl.getId()))
				.andExpect(jsonPath("$.shortCode").value("details"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/details"));
	}

	@Test
	void shouldHideUrlOwnedByAnotherUser() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "private", "https://example.com/private", false, null));

		mockMvc.perform(get("/api/v1/urls/{id}", shortUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldRejectInvalidPaginationAndRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/urls")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/urls"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldDisableAndReEnableOwnedUrl() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "toggle1", "https://example.com/toggle", false, null));

		updateStatus(shortUrl.getId(), "DISABLED")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISABLED"));

		mockMvc.perform(get("/{shortCode}", "toggle1"))
				.andExpect(status().isGone());

		updateStatus(shortUrl.getId(), "ACTIVE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void shouldNotUpdateAnotherUsersUrlOrAcceptBlockedStatus() throws Exception {
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "foreign2", "https://example.com/foreign", false, null));

		updateStatus(foreignUrl.getId(), "DISABLED")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));

		ShortUrl ownedUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "owned03", "https://example.com/owned", false, null));

		updateStatus(ownedUrl.getId(), "BLOCKED")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

		ownedUrl.block();
		shortUrlRepository.saveAndFlush(ownedUrl);

		updateStatus(ownedUrl.getId(), "ACTIVE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SHORT_URL_BLOCKED"));
	}

	private org.springframework.test.web.servlet.ResultActions updateStatus(
			Long id,
			String newStatus) throws Exception {
		return mockMvc.perform(patch("/api/v1/urls/{id}/status", id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "%s"}
						""".formatted(newStatus)));
	}
}

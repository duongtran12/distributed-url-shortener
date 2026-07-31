package com.duong.url_shortener.shorturl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RedirectIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	private User owner;

	@BeforeEach
	void setUp() {
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		owner = userRepository.saveAndFlush(
				User.create("owner@example.com", "encoded-password", "URL Owner"));
	}

	@Test
	void shouldRedirectPublicRequestToOriginalUrl() throws Exception {
		shortUrlRepository.saveAndFlush(ShortUrl.create(
				owner,
				"abc1234",
				"https://example.com/long/path?source=test",
				false,
				null));

		mockMvc.perform(get("/abc1234"))
				.andExpect(status().isFound())
				.andExpect(header().string(
						HttpHeaders.LOCATION,
						"https://example.com/long/path?source=test"));
	}

	@Test
	void shouldReturnNotFoundForUnknownCode() throws Exception {
		mockMvc.perform(get("/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldReturnGoneForDisabledUrl() throws Exception {
		ShortUrl shortUrl = ShortUrl.create(
				owner,
				"disabled",
				"https://example.com",
				true,
				null);
		shortUrl.disable();
		shortUrlRepository.saveAndFlush(shortUrl);

		mockMvc.perform(get("/disabled"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("SHORT_URL_UNAVAILABLE"));
	}

	@Test
	void shouldReturnGoneForExpiredUrl() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(ShortUrl.create(
				owner,
				"expires1",
				"https://example.com",
				false,
				Instant.now().plusSeconds(1)));

		Thread.sleep(1100);

		mockMvc.perform(get("/expires1"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("SHORT_URL_UNAVAILABLE"));
	}
}

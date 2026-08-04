package com.duong.url_shortener.click;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.duong.url_shortener.security.JwtTokenService;
import com.duong.url_shortener.shorturl.ShortUrl;
import com.duong.url_shortener.shorturl.ShortUrlRepository;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AnalyticsOverviewIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClickEventStore clickEventStore;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	private User owner;
	private String ownerToken;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM click_events");
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		owner = userRepository.saveAndFlush(
				User.create("overview-owner@example.com", "encoded-password", "Overview Owner"));
		ownerToken = jwtTokenService.createAccessToken(owner);
	}

	@Test
	void shouldReturnOwnerOverviewAndRankTopUrls() throws Exception {
		ShortUrl mostClicked = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "popular1", "https://example.com/popular", false, null));
		ShortUrl second = ShortUrl.create(
				owner, "second01", "https://example.com/second", false, null);
		second.disable();
		shortUrlRepository.saveAndFlush(second);

		User anotherOwner = userRepository.saveAndFlush(
				User.create("overview-other@example.com", "encoded-password", "Other Owner"));
		shortUrlRepository.saveAndFlush(
				ShortUrl.create(anotherOwner, "foreign1", "https://example.com/foreign", false, null));

		Instant clickTime = Instant.now().minusSeconds(5);
		record("popular1", clickTime, "visitor-one");
		record("popular1", clickTime.plusSeconds(1), "visitor-one");
		record("popular1", clickTime.plusSeconds(2), "visitor-two");
		record("second01", clickTime.plusSeconds(3), "visitor-one");
		record("foreign1", clickTime.plusSeconds(4), "foreign-visitor");

		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		mockMvc.perform(get("/api/v1/analytics/overview")
				.param("from", today.minusDays(1).toString())
				.param("to", today.toString())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUrls").value(2))
				.andExpect(jsonPath("$.activeUrls").value(1))
				.andExpect(jsonPath("$.lifetimeClicks").value(4))
				.andExpect(jsonPath("$.periodClicks").value(4))
				.andExpect(jsonPath("$.periodUniqueVisitors").value(2))
				.andExpect(jsonPath("$.dailyClicks.length()").value(2))
				.andExpect(jsonPath("$.dailyClicks[0].clicks").value(0))
				.andExpect(jsonPath("$.dailyClicks[1].clicks").value(4))
				.andExpect(jsonPath("$.topUrls.length()").value(2))
				.andExpect(jsonPath("$.topUrls[0].shortUrlId").value(mostClicked.getId()))
				.andExpect(jsonPath("$.topUrls[0].shortCode").value("popular1"))
				.andExpect(jsonPath("$.topUrls[0].clicks").value(3))
				.andExpect(jsonPath("$.topUrls[0].uniqueVisitors").value(2))
				.andExpect(jsonPath("$.topUrls[1].shortCode").value("second01"));
	}

	@Test
	void shouldRejectInvalidOverviewRange() throws Exception {
		mockMvc.perform(get("/api/v1/analytics/overview")
				.param("from", "2030-02-01")
				.param("to", "2030-01-01")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ANALYTICS_RANGE"));
	}

	private void record(String shortCode, Instant clickedAt, String visitorHash) {
		clickEventStore.record(ClickEvent.create(
				shortCode, clickedAt, null, null, visitorHash));
	}
}

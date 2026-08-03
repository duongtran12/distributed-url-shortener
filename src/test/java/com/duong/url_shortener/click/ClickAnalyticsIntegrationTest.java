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
class ClickAnalyticsIntegrationTest {

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
	private User otherUser;
	private String ownerToken;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM click_events");
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		owner = userRepository.saveAndFlush(
				User.create("analytics-owner@example.com", "encoded-password", "Analytics Owner"));
		otherUser = userRepository.saveAndFlush(
				User.create("analytics-other@example.com", "encoded-password", "Other Owner"));
		ownerToken = jwtTokenService.createAccessToken(owner);
	}

	@Test
	void shouldReturnLifetimeAndDailyClickAnalytics() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "stats01", "https://example.com/stats", false, null));
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		Instant firstClick = Instant.now().plusSeconds(1);
		String chromeWindows = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
				+ "Chrome/126.0.0.0 Safari/537.36";
		String safariIphone = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
				+ "Mobile/15E148 Version/17.0 Safari/604.1";
		clickEventStore.record(ClickEvent.create(
				"stats01", firstClick, chromeWindows, "https://google.com/search?q=one"));
		clickEventStore.record(ClickEvent.create(
				"stats01", firstClick.plusSeconds(60), chromeWindows, "https://google.com/other"));
		clickEventStore.record(ClickEvent.create(
				"stats01", firstClick.plusSeconds(120), safariIphone, null));

		mockMvc.perform(get("/api/v1/urls/{id}/analytics", shortUrl.getId())
				.param("from", today.minusDays(2).toString())
				.param("to", today.toString())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("stats01"))
				.andExpect(jsonPath("$.lifetimeClicks").value(3))
				.andExpect(jsonPath("$.periodClicks").value(3))
				.andExpect(jsonPath("$.dailyClicks.length()").value(3))
				.andExpect(jsonPath("$.dailyClicks[0].clicks").value(0))
				.andExpect(jsonPath("$.dailyClicks[1].clicks").value(0))
				.andExpect(jsonPath("$.dailyClicks[2].clicks").value(3))
				.andExpect(jsonPath("$.browsers[0].category").value("Chrome"))
				.andExpect(jsonPath("$.browsers[0].clicks").value(2))
				.andExpect(jsonPath("$.browsers[1].category").value("Safari"))
				.andExpect(jsonPath("$.operatingSystems[0].category").value("Windows"))
				.andExpect(jsonPath("$.devices[0].category").value("DESKTOP"))
				.andExpect(jsonPath("$.devices[1].category").value("MOBILE"))
				.andExpect(jsonPath("$.referrers[0].category").value("google.com"))
				.andExpect(jsonPath("$.referrers[0].clicks").value(2))
				.andExpect(jsonPath("$.referrers[1].category").value("direct"));
	}

	@Test
	void shouldHideAnalyticsForAnotherUsersUrl() throws Exception {
		ShortUrl foreignUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(otherUser, "stats02", "https://example.com/foreign", false, null));

		mockMvc.perform(get("/api/v1/urls/{id}/analytics", foreignUrl.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void shouldRejectInvalidAndOversizedDateRanges() throws Exception {
		ShortUrl shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "stats03", "https://example.com/range", false, null));

		mockMvc.perform(get("/api/v1/urls/{id}/analytics", shortUrl.getId())
				.param("from", "2030-02-01")
				.param("to", "2030-01-01")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ANALYTICS_RANGE"));

		mockMvc.perform(get("/api/v1/urls/{id}/analytics", shortUrl.getId())
				.param("from", "2028-01-01")
				.param("to", "2030-01-01")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ANALYTICS_RANGE_TOO_LARGE"));
	}
}

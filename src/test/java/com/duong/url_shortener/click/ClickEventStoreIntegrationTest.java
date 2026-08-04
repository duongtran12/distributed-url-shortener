package com.duong.url_shortener.click;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import com.duong.url_shortener.shorturl.ShortUrl;
import com.duong.url_shortener.shorturl.ShortUrlRepository;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
@Testcontainers
class ClickEventStoreIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private ClickEventStore clickEventStore;

	@Autowired
	private ShortUrlRepository shortUrlRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ShortUrl shortUrl;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM click_events");
		shortUrlRepository.deleteAll();
		userRepository.deleteAll();
		User owner = userRepository.saveAndFlush(
				User.create("click-owner@example.com", "encoded-password", "Click Owner"));
		shortUrl = shortUrlRepository.saveAndFlush(
				ShortUrl.create(owner, "tracked", "https://example.com", false, null));
	}

	@Test
	void shouldIncrementClickCountOnceForDuplicateDelivery() {
		ClickEvent event = new ClickEvent(
				UUID.randomUUID(),
				"tracked",
				Instant.parse("2030-01-01T00:00:00Z"),
				"Mozilla/5.0 (Windows NT 10.0) Chrome/126.0 Safari/537.36",
				"https://github.com/private/path",
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

		clickEventStore.record(event);
		clickEventStore.record(event);

		ShortUrl updated = shortUrlRepository.findById(shortUrl.getId()).orElseThrow();
		assertEquals(1, updated.getClickCount());
		assertEquals("Chrome", jdbcTemplate.queryForObject(
				"SELECT browser FROM click_events WHERE event_id = ?",
				String.class,
				event.eventId()));
		assertEquals("github.com", jdbcTemplate.queryForObject(
				"SELECT referrer FROM click_events WHERE event_id = ?",
				String.class,
				event.eventId()));
		assertEquals(event.visitorHash(), jdbcTemplate.queryForObject(
				"SELECT visitor_hash FROM click_events WHERE event_id = ?",
				String.class,
				event.eventId()));
	}
}

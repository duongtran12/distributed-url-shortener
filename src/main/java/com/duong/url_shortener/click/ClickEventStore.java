package com.duong.url_shortener.click;

import java.sql.Timestamp;

import com.duong.url_shortener.shorturl.ShortUrlRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClickEventStore {

	private final JdbcTemplate jdbcTemplate;
	private final ShortUrlRepository shortUrlRepository;

	public ClickEventStore(JdbcTemplate jdbcTemplate, ShortUrlRepository shortUrlRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.shortUrlRepository = shortUrlRepository;
	}

	@Transactional
	public void record(ClickEvent event) {
		int inserted = jdbcTemplate.update("""
				INSERT INTO click_events (event_id, short_code, clicked_at)
				VALUES (?, ?, ?)
				ON CONFLICT (event_id) DO NOTHING
				""", event.eventId(), event.shortCode(), Timestamp.from(event.clickedAt()));

		if (inserted == 1) {
			shortUrlRepository.incrementClickCount(event.shortCode());
		}
	}
}

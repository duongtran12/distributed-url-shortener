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
	private final ClickMetadataExtractor metadataExtractor;

	public ClickEventStore(
			JdbcTemplate jdbcTemplate,
			ShortUrlRepository shortUrlRepository,
			ClickMetadataExtractor metadataExtractor) {
		this.jdbcTemplate = jdbcTemplate;
		this.shortUrlRepository = shortUrlRepository;
		this.metadataExtractor = metadataExtractor;
	}

	@Transactional
	public void record(ClickEvent event) {
		ClickMetadata metadata = metadataExtractor.extract(event);
		int inserted = jdbcTemplate.update("""
				INSERT INTO click_events (
				    event_id, short_code, clicked_at, user_agent,
				    referrer, browser, operating_system, device_type)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (event_id) DO NOTHING
				""",
				event.eventId(),
				event.shortCode(),
				Timestamp.from(event.clickedAt()),
				metadata.userAgent(),
				metadata.referrer(),
				metadata.browser(),
				metadata.operatingSystem(),
				metadata.deviceType().name());

		if (inserted == 1) {
			shortUrlRepository.incrementClickCount(event.shortCode());
		}
	}
}

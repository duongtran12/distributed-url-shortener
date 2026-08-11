package com.duong.url_shortener.click;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClickEventRetentionRepository {

	private final JdbcTemplate jdbcTemplate;

	public ClickEventRetentionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int deleteOldestBatch(Instant cutoff, int batchSize) {
		return jdbcTemplate.update("""
				DELETE FROM click_events
				WHERE event_id IN (
				    SELECT event_id
				    FROM click_events
				    WHERE clicked_at < ?
				    ORDER BY clicked_at, event_id
				    LIMIT ?
				)
				""", Timestamp.from(cutoff), batchSize);
	}
}

package com.duong.url_shortener.auth;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegistrationCleanupRepository {

	private final JdbcTemplate jdbcTemplate;

	public RegistrationCleanupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int deleteOldestBatch(Instant cutoff, int batchSize) {
		return jdbcTemplate.update("""
				DELETE FROM users
				WHERE id IN (
				    SELECT user_account.id
				    FROM users user_account
				    INNER JOIN email_verification_tokens verification
				        ON verification.user_id = user_account.id
				    WHERE user_account.enabled = FALSE
				      AND user_account.created_at < ?
				    ORDER BY user_account.created_at, user_account.id
				    LIMIT ?
				)
				""", Timestamp.from(cutoff), batchSize);
	}
}

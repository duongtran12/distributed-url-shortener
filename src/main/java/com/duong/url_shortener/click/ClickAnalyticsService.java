package com.duong.url_shortener.click;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.shorturl.ShortUrl;
import com.duong.url_shortener.shorturl.ShortUrlRepository;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickAnalyticsService {

	private static final long MAX_RANGE_DAYS = 366;

	private final UserRepository userRepository;
	private final ShortUrlRepository shortUrlRepository;
	private final JdbcTemplate jdbcTemplate;

	public ClickAnalyticsService(
			UserRepository userRepository,
			ShortUrlRepository shortUrlRepository,
			JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.shortUrlRepository = shortUrlRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public ClickAnalyticsResponse getAnalytics(
			Long userId,
			Long shortUrlId,
			LocalDate requestedFrom,
			LocalDate requestedTo) {
		findActiveUser(userId);
		ShortUrl shortUrl = shortUrlRepository.findByIdAndOwnerId(shortUrlId, userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"SHORT_URL_NOT_FOUND",
						"Short URL was not found"));

		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate to = requestedTo == null ? today : requestedTo;
		LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;
		validateRange(from, to);

		Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();
		from.datesUntil(to.plusDays(1)).forEach(date -> countsByDate.put(date, 0L));

		Instant requestedStart = from.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant effectiveStart = requestedStart.isAfter(shortUrl.getCreatedAt())
				? requestedStart
				: shortUrl.getCreatedAt();
		Instant endExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

		if (effectiveStart.isBefore(endExclusive)) {
			jdbcTemplate.query("""
					SELECT (clicked_at AT TIME ZONE 'UTC')::date AS click_date,
					       COUNT(*) AS clicks
					FROM click_events
					WHERE short_code = ?
					  AND clicked_at >= ?
					  AND clicked_at < ?
					GROUP BY click_date
					ORDER BY click_date
					""",
					resultSet -> {
						LocalDate date = resultSet.getObject("click_date", LocalDate.class);
						countsByDate.put(date, resultSet.getLong("clicks"));
					},
					shortUrl.getShortCode(),
					Timestamp.from(effectiveStart),
					Timestamp.from(endExclusive));
		}

		List<DailyClickCount> dailyClicks = countsByDate.entrySet().stream()
				.map(entry -> new DailyClickCount(entry.getKey(), entry.getValue()))
				.toList();
		long periodClicks = dailyClicks.stream().mapToLong(DailyClickCount::clicks).sum();

		return new ClickAnalyticsResponse(
				shortUrl.getId(),
				shortUrl.getShortCode(),
				shortUrl.getClickCount(),
				periodClicks,
				from,
				to,
				dailyClicks);
	}

	private void validateRange(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"INVALID_ANALYTICS_RANGE",
					"Analytics start date must not be after end date");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"ANALYTICS_RANGE_TOO_LARGE",
					"Analytics date range must not exceed 366 days");
		}
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_ACCESS_TOKEN",
						"The access token no longer belongs to an existing user"));
		if (!user.isEnabled()) {
			throw new ApiException(
					HttpStatus.FORBIDDEN,
					"ACCOUNT_DISABLED",
					"The user account is disabled");
		}
		return user;
	}
}

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
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsOverviewService {

	private static final long MAX_RANGE_DAYS = 366;
	private static final int TOP_URL_LIMIT = 10;

	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;

	public AnalyticsOverviewService(
			UserRepository userRepository,
			JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public AnalyticsOverviewResponse getOverview(
			Long userId,
			LocalDate requestedFrom,
			LocalDate requestedTo) {
		findActiveUser(userId);

		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate to = requestedTo == null ? today : requestedTo;
		LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;
		validateRange(from, to);

		Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant endExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		Timestamp now = Timestamp.from(Instant.now());

		OverviewTotals totals = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) AS total_urls,
				       COUNT(*) FILTER (
				           WHERE status = 'ACTIVE'
				             AND (expires_at IS NULL OR expires_at > ?)
				       ) AS active_urls,
				       COALESCE(SUM(click_count), 0) AS lifetime_clicks
				FROM short_urls
				WHERE user_id = ?
				""",
				(resultSet, rowNumber) -> new OverviewTotals(
						resultSet.getLong("total_urls"),
						resultSet.getLong("active_urls"),
						resultSet.getLong("lifetime_clicks")),
				now,
				userId);

		PeriodTotals periodTotals = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) AS period_clicks,
				       COUNT(DISTINCT event.visitor_hash) AS unique_visitors
				FROM click_events event
				JOIN short_urls url ON url.short_code = event.short_code
				WHERE url.user_id = ?
				  AND event.clicked_at >= ?
				  AND event.clicked_at < ?
				""",
				(resultSet, rowNumber) -> new PeriodTotals(
						resultSet.getLong("period_clicks"),
						resultSet.getLong("unique_visitors")),
				userId,
				Timestamp.from(start),
				Timestamp.from(endExclusive));

		Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();
		from.datesUntil(to.plusDays(1)).forEach(date -> countsByDate.put(date, 0L));
		jdbcTemplate.query("""
				SELECT (event.clicked_at AT TIME ZONE 'UTC')::date AS click_date,
				       COUNT(*) AS clicks
				FROM click_events event
				JOIN short_urls url ON url.short_code = event.short_code
				WHERE url.user_id = ?
				  AND event.clicked_at >= ?
				  AND event.clicked_at < ?
				GROUP BY click_date
				ORDER BY click_date
				""",
				resultSet -> {
					countsByDate.put(
							resultSet.getObject("click_date", LocalDate.class),
							resultSet.getLong("clicks"));
				},
				userId,
				Timestamp.from(start),
				Timestamp.from(endExclusive));
		List<DailyClickCount> dailyClicks = countsByDate.entrySet().stream()
				.map(entry -> new DailyClickCount(entry.getKey(), entry.getValue()))
				.toList();

		List<TopUrlAnalytics> topUrls = jdbcTemplate.query("""
				SELECT url.id,
				       url.short_code,
				       url.original_url,
				       COUNT(*) AS clicks,
				       COUNT(DISTINCT event.visitor_hash) AS unique_visitors
				FROM click_events event
				JOIN short_urls url ON url.short_code = event.short_code
				WHERE url.user_id = ?
				  AND event.clicked_at >= ?
				  AND event.clicked_at < ?
				GROUP BY url.id, url.short_code, url.original_url
				ORDER BY clicks DESC, url.id ASC
				LIMIT ?
				""",
				(resultSet, rowNumber) -> new TopUrlAnalytics(
						resultSet.getLong("id"),
						resultSet.getString("short_code"),
						resultSet.getString("original_url"),
						resultSet.getLong("clicks"),
						resultSet.getLong("unique_visitors")),
				userId,
				Timestamp.from(start),
				Timestamp.from(endExclusive),
				TOP_URL_LIMIT);

		return new AnalyticsOverviewResponse(
				totals.totalUrls(),
				totals.activeUrls(),
				totals.lifetimeClicks(),
				periodTotals.periodClicks(),
				periodTotals.uniqueVisitors(),
				from,
				to,
				dailyClicks,
				topUrls);
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

	private record OverviewTotals(long totalUrls, long activeUrls, long lifetimeClicks) {
	}

	private record PeriodTotals(long periodClicks, long uniqueVisitors) {
	}
}

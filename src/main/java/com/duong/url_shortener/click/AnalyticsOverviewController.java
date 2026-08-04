package com.duong.url_shortener.click;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/overview")
public class AnalyticsOverviewController {

	private final AnalyticsOverviewService analyticsOverviewService;

	public AnalyticsOverviewController(AnalyticsOverviewService analyticsOverviewService) {
		this.analyticsOverviewService = analyticsOverviewService;
	}

	@GetMapping
	public AnalyticsOverviewResponse getOverview(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return analyticsOverviewService.getOverview(jwt.getClaim("uid"), from, to);
	}
}

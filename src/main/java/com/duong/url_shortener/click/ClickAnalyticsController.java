package com.duong.url_shortener.click;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls/{id}/analytics")
@Validated
public class ClickAnalyticsController {

	private final ClickAnalyticsService clickAnalyticsService;

	public ClickAnalyticsController(ClickAnalyticsService clickAnalyticsService) {
		this.clickAnalyticsService = clickAnalyticsService;
	}

	@GetMapping
	public ClickAnalyticsResponse getAnalytics(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return clickAnalyticsService.getAnalytics(jwt.getClaim("uid"), id, from, to);
	}
}

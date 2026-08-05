package com.duong.url_shortener.click;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	private final AnalyticsCsvExportService analyticsCsvExportService;

	public AnalyticsOverviewController(
			AnalyticsOverviewService analyticsOverviewService,
			AnalyticsCsvExportService analyticsCsvExportService) {
		this.analyticsOverviewService = analyticsOverviewService;
		this.analyticsCsvExportService = analyticsCsvExportService;
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

	@GetMapping(value = "/export", produces = "text/csv")
	public ResponseEntity<byte[]> exportOverview(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		AnalyticsCsvExportService.AnalyticsCsvExport export =
				analyticsCsvExportService.export(jwt.getClaim("uid"), from, to);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(export.filename()).build().toString())
				.body(export.content());
	}
}

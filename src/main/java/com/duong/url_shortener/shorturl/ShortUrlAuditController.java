package com.duong.url_shortener.shorturl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit/short-urls")
@Validated
public class ShortUrlAuditController {

	private final ShortUrlAuditService auditService;

	public ShortUrlAuditController(ShortUrlAuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping
	public ShortUrlAuditPageResponse findAll(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) ShortUrlAuditAction action,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return auditService.findAll(jwt.getClaim("uid"), action, page, size);
	}
}

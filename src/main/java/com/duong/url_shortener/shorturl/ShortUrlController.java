package com.duong.url_shortener.shorturl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class ShortUrlController {

	private final ShortUrlService shortUrlService;

	public ShortUrlController(ShortUrlService shortUrlService) {
		this.shortUrlService = shortUrlService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ShortUrlResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateShortUrlRequest request) {
		return shortUrlService.create(jwt.getClaim("uid"), request);
	}

	@GetMapping
	public ShortUrlPageResponse findAll(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return shortUrlService.findAllOwnedBy(jwt.getClaim("uid"), page, size);
	}

	@GetMapping("/{id}")
	public ShortUrlResponse findById(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id) {
		return shortUrlService.findOwnedById(jwt.getClaim("uid"), id);
	}

	@PatchMapping("/{id}/status")
	public ShortUrlResponse updateStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Min(1) Long id,
			@Valid @RequestBody UpdateShortUrlStatusRequest request) {
		return shortUrlService.updateStatus(jwt.getClaim("uid"), id, request);
	}
}

package com.duong.url_shortener.shorturl;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
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
}

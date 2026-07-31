package com.duong.url_shortener.shorturl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final RedirectService redirectService;

	public RedirectController(RedirectService redirectService) {
		this.redirectService = redirectService;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, redirectService.resolve(shortCode).toString())
				.build();
	}
}

package com.duong.url_shortener.shorturl;

import java.time.Instant;

import com.duong.url_shortener.click.ClickEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final RedirectService redirectService;
	private final ClickEventPublisher clickEventPublisher;

	public RedirectController(
			RedirectService redirectService,
			ClickEventPublisher clickEventPublisher) {
		this.redirectService = redirectService;
		this.clickEventPublisher = clickEventPublisher;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		String destination = redirectService.resolve(shortCode).toString();
		clickEventPublisher.publish(shortCode, Instant.now());
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, destination)
				.build();
	}
}

package com.duong.url_shortener.shorturl;

import java.time.Instant;

import com.duong.url_shortener.click.ClickEventPublisher;
import com.duong.url_shortener.click.VisitorFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final RedirectService redirectService;
	private final ClickEventPublisher clickEventPublisher;
	private final VisitorFingerprintService visitorFingerprintService;

	public RedirectController(
			RedirectService redirectService,
			ClickEventPublisher clickEventPublisher,
			VisitorFingerprintService visitorFingerprintService) {
		this.redirectService = redirectService;
		this.clickEventPublisher = clickEventPublisher;
		this.visitorFingerprintService = visitorFingerprintService;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(
			@PathVariable String shortCode,
			@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
			@RequestHeader(value = HttpHeaders.REFERER, required = false) String referrer,
			HttpServletRequest request) {
		String destination = redirectService.resolve(shortCode).toString();
		String visitorHash = visitorFingerprintService.hash(request.getRemoteAddr());
		clickEventPublisher.publish(
				shortCode, Instant.now(), userAgent, referrer, visitorHash);
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, destination)
				.build();
	}
}

package com.duong.url_shortener.auth;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

	private final RefreshTokenService refreshTokenService;
	private final RefreshTokenProperties properties;

	public SessionController(RefreshTokenService refreshTokenService, RefreshTokenProperties properties) {
		this.refreshTokenService = refreshTokenService;
		this.properties = properties;
	}

	@GetMapping
	public List<SessionResponse> findAll(
			@AuthenticationPrincipal Jwt jwt,
			@CookieValue(name = "shortwave_refresh", required = false) String refreshToken) {
		return refreshTokenService.findActiveSessions(jwt.getClaim("uid"), refreshToken);
	}

	@DeleteMapping("/{sessionId}")
	public ResponseEntity<Void> revoke(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long sessionId,
			@CookieValue(name = "shortwave_refresh", required = false) String refreshToken) {
		boolean current = refreshTokenService.revokeSession(jwt.getClaim("uid"), sessionId, refreshToken);
		if (current) {
			return ResponseEntity.noContent()
					.header(HttpHeaders.SET_COOKIE, ResponseCookie.from("shortwave_refresh", "")
					.httpOnly(true)
					.secure(properties.secureCookie())
					.sameSite("Strict")
					.path("/api/v1/auth")
					.maxAge(0)
					.build().toString())
					.build();
		}
		return ResponseEntity.noContent().build();
	}
}

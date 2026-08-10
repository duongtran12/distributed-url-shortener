package com.duong.url_shortener.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenProperties refreshTokenProperties;
	private final PasswordResetService passwordResetService;

	public AuthController(
			AuthService authService,
			RefreshTokenProperties refreshTokenProperties,
			PasswordResetService passwordResetService) {
		this.authService = authService;
		this.refreshTokenProperties = refreshTokenProperties;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(
			@Valid @RequestBody LoginRequest request,
			@RequestHeader(name = HttpHeaders.USER_AGENT, required = false) String userAgent) {
		return sessionResponse(authService.login(request, userAgent));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(
			@CookieValue(name = "shortwave_refresh", required = false) String refreshToken,
			@RequestHeader(name = HttpHeaders.USER_AGENT, required = false) String userAgent) {
		return sessionResponse(authService.refresh(refreshToken == null ? "" : refreshToken, userAgent));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = "shortwave_refresh", required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
				.build();
	}

	@PostMapping("/password-reset/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestReset(request);
	}

	@PostMapping("/password-reset/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		passwordResetService.resetPassword(request);
	}

	private ResponseEntity<LoginResponse> sessionResponse(AuthSession session) {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(
						session.refreshToken(), refreshTokenProperties.expiration().toSeconds()).toString())
				.body(session.loginResponse());
	}

	private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
		return ResponseCookie.from("shortwave_refresh", value)
				.httpOnly(true)
				.secure(refreshTokenProperties.secureCookie())
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(maxAgeSeconds)
				.build();
	}
}

package com.duong.url_shortener.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.duong.url_shortener.common.exception.ApiException;
import com.duong.url_shortener.security.JwtTokenService;
import com.duong.url_shortener.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final RefreshTokenRepository repository;
	private final RefreshTokenProperties properties;
	private final JwtTokenService jwtTokenService;
	private final Clock clock;

	public RefreshTokenService(
			RefreshTokenRepository repository,
			RefreshTokenProperties properties,
			JwtTokenService jwtTokenService,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.jwtTokenService = jwtTokenService;
		this.clock = clock;
	}

	@Transactional
	public AuthSession create(User user) {
		String rawToken = generateToken();
		repository.save(RefreshToken.create(user, hash(rawToken), clock.instant().plus(properties.expiration())));
		return session(user, rawToken);
	}

	@Transactional
	public AuthSession rotate(String rawToken) {
		Instant now = clock.instant();
		RefreshToken current = repository.findByTokenHash(hash(rawToken))
				.orElseThrow(this::invalidRefreshToken);
		if (!current.isUsableAt(now)) {
			throw invalidRefreshToken();
		}
		current.revoke(now);
		return create(current.getUser());
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) return;
		Instant now = clock.instant();
		repository.findByTokenHash(hash(rawToken))
				.filter(token -> token.isUsableAt(now))
				.ifPresent(token -> token.revoke(now));
	}

	@Transactional
	public void revokeAllForUser(Long userId) {
		repository.revokeAllByUserId(userId, clock.instant());
	}

	private AuthSession session(User user, String rawToken) {
		return new AuthSession(new LoginResponse(
				jwtTokenService.createAccessToken(user), "Bearer", jwtTokenService.accessTokenExpiresInSeconds()), rawToken);
	}

	private String generateToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private ApiException invalidRefreshToken() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The session cannot be refreshed");
	}
}

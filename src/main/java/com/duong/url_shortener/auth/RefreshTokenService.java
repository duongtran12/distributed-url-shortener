package com.duong.url_shortener.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

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
	public AuthSession create(User user, String userAgent) {
		Instant now = clock.instant();
		String rawToken = generateToken();
		RefreshToken token = repository.saveAndFlush(RefreshToken.create(
				user, hash(rawToken), now.plus(properties.expiration()), normalizeUserAgent(userAgent), now));
		repository.revokeSessionsExceedingLimit(
				user.getId(), token.getId(), now, properties.maxActiveSessions());
		return session(user, rawToken);
	}

	@Transactional
	public AuthSession rotate(String rawToken, String userAgent) {
		Instant now = clock.instant();
		RefreshToken current = repository.findByTokenHash(hash(rawToken))
				.orElseThrow(this::invalidRefreshToken);
		if (!current.isUsableAt(now)) {
			throw invalidRefreshToken();
		}
		current.revoke(now);
		return create(current.getUser(), userAgent);
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

	@Transactional(readOnly = true)
	public List<SessionResponse> findActiveSessions(Long userId, String currentRawToken) {
		String currentHash = currentRawToken == null || currentRawToken.isBlank() ? "" : hash(currentRawToken);
		return repository.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
				userId, clock.instant()).stream()
				.map(token -> SessionResponse.from(token, currentHash))
				.toList();
	}

	@Transactional
	public boolean revokeSession(Long userId, Long sessionId, String currentRawToken) {
		RefreshToken token = repository.findByIdAndUserId(sessionId, userId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "The session was not found"));
		boolean current = currentRawToken != null
				&& !currentRawToken.isBlank()
				&& token.getTokenHash().equals(hash(currentRawToken));
		if (token.isUsableAt(clock.instant())) token.revoke(clock.instant());
		return current;
	}

	@Transactional
	public int revokeOtherSessions(Long userId, String currentRawToken) {
		if (currentRawToken == null || currentRawToken.isBlank()) throw invalidRefreshToken();
		Instant now = clock.instant();
		RefreshToken current = repository.findByTokenHash(hash(currentRawToken))
				.orElseThrow(this::invalidRefreshToken);
		if (!current.isUsableAt(now) || !current.getUser().getId().equals(userId)) {
			throw invalidRefreshToken();
		}
		return repository.revokeAllOtherSessions(userId, current.getId(), now);
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

	private String normalizeUserAgent(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) return "Unknown client";
		String normalized = userAgent.strip();
		return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
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

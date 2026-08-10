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
import com.duong.url_shortener.user.EmailNormalizer;
import com.duong.url_shortener.user.User;
import com.duong.url_shortener.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final PasswordResetTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final PasswordResetProperties properties;
	private final JavaMailSender mailSender;
	private final Clock clock;

	public PasswordResetService(
			PasswordResetTokenRepository tokenRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService,
			PasswordResetProperties properties,
			JavaMailSender mailSender,
			Clock clock) {
		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.properties = properties;
		this.mailSender = mailSender;
		this.clock = clock;
	}

	@Transactional
	public void requestReset(PasswordResetRequest request) {
		userRepository.findByEmail(EmailNormalizer.normalize(request.email()))
				.filter(User::isEnabled)
				.ifPresent(this::createAndSendToken);
	}

	@Transactional
	public void resetPassword(PasswordResetConfirmRequest request) {
		Instant now = clock.instant();
		PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token()))
				.orElseThrow(this::invalidToken);
		if (!token.isUsableAt(now)) throw invalidToken();

		User user = token.getUser();
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"PASSWORD_UNCHANGED",
					"The new password must be different from the current password");
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		tokenRepository.consumeAllForUser(user.getId(), now);
		refreshTokenService.revokeAllForUser(user.getId());
	}

	private void createAndSendToken(User user) {
		Instant now = clock.instant();
		tokenRepository.consumeAllForUser(user.getId(), now);
		String rawToken = generateToken();
		tokenRepository.save(PasswordResetToken.create(
				user, hash(rawToken), now.plus(properties.expiration())));

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.fromAddress());
		message.setTo(user.getEmail());
		message.setSubject("Reset your Shortwave password");
		message.setText("Use this link within %d minutes to reset your password:\n\n%s?resetToken=%s\n\n"
				.formatted(properties.expiration().toMinutes(), properties.frontendUrl(), rawToken));
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			log.error("Could not send password reset email", exception);
		}
	}

	private String generateToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private ApiException invalidToken() {
		return new ApiException(
				HttpStatus.BAD_REQUEST,
				"INVALID_PASSWORD_RESET_TOKEN",
				"The password reset link is invalid or has expired");
	}
}

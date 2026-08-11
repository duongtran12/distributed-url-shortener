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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

	private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final EmailVerificationTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final EmailVerificationProperties properties;
	private final JavaMailSender mailSender;
	private final Clock clock;

	public EmailVerificationService(
			EmailVerificationTokenRepository tokenRepository,
			UserRepository userRepository,
			EmailVerificationProperties properties,
			JavaMailSender mailSender,
			Clock clock) {
		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
		this.properties = properties;
		this.mailSender = mailSender;
		this.clock = clock;
	}

	@Transactional
	public void sendVerification(User user) {
		if (user.isEnabled()) return;
		tokenRepository.deleteByUserId(user.getId());
		String rawToken = generateToken();
		tokenRepository.save(EmailVerificationToken.create(
				user, hash(rawToken), clock.instant().plus(properties.expiration())));
		sendMessage(user, rawToken);
	}

	@Transactional
	public void requestVerification(EmailVerificationRequest request) {
		userRepository.findByEmail(EmailNormalizer.normalize(request.email()))
				.filter(user -> !user.isEnabled())
				.ifPresent(this::sendVerification);
	}

	@Transactional
	public void confirm(EmailVerificationConfirmRequest request) {
		Instant now = clock.instant();
		EmailVerificationToken token = tokenRepository.findByTokenHash(hash(request.token()))
				.orElseThrow(this::invalidToken);
		if (!token.isUsableAt(now)) throw invalidToken();

		User user = token.getUser();
		user.enable();
		tokenRepository.deleteByUserId(user.getId());
	}

	private void sendMessage(User user, String rawToken) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.fromAddress());
		message.setTo(user.getEmail());
		message.setSubject("Verify your Shortwave email");
		message.setText("Use this link within %d hours to verify your email:\n\n%s?verificationToken=%s\n\n"
				.formatted(properties.expiration().toHours(), properties.frontendUrl(), rawToken));
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			log.error("Could not send email verification message", exception);
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
				"INVALID_EMAIL_VERIFICATION_TOKEN",
				"The email verification link is invalid or has expired");
	}
}
